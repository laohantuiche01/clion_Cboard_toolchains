import com.github.waveformplotter.*
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.MessageBusConnection
import com.intellij.xdebugger.*
import com.intellij.xdebugger.breakpoints.XBreakpointManager
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.runners.ExecutionEnvironment
import java.lang.reflect.Proxy
import java.awt.event.MouseEvent
import javax.swing.*

private class TestManager(private val session: XDebugSession) : XDebuggerManager() {
    override fun getCurrentSession() = session
    override fun getDebugSessions() = arrayOf(session)
    override fun getDebugSession(console: ExecutionConsole): XDebugSession? = session
    override fun <T : XDebugProcess> getDebugProcesses(type: Class<T>): List<T> = emptyList()
    override fun getBreakpointManager(): XBreakpointManager = error("unused")
    override fun newSessionBuilder(starter: XDebugProcessStarter): XDebugSessionBuilder = error("unused")
    override fun startSession(env: ExecutionEnvironment, starter: XDebugProcessStarter): XDebugSession = error("unused")
    override fun startSessionAndShowTab(name: String, descriptor: RunContentDescriptor?, starter: XDebugProcessStarter): XDebugSession = error("unused")
    override fun startSessionAndShowTab(name: String, starter: XDebugProcessStarter, env: ExecutionEnvironment): XDebugSession = error("unused")
    override fun startSessionAndShowTab(name: String, descriptor: RunContentDescriptor?, suspended: Boolean, starter: XDebugProcessStarter): XDebugSession = error("unused")
    override fun startSessionAndShowTab(name: String, icon: Icon?, descriptor: RunContentDescriptor?, suspended: Boolean, starter: XDebugProcessStarter): XDebugSession = error("unused")
}

private inline fun <reified T> fake(crossinline handler: (Any, java.lang.reflect.Method, Array<out Any?>?) -> Any?): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { proxy, method, args ->
        when (method.name) {
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            "toString" -> "Test${T::class.java.simpleName}"
            else -> handler(proxy, method, args)
        }
    } as T

private fun member(target: Any, name: String): Any? = target.javaClass.getDeclaredField(name).let {
    it.isAccessible = true
    it.get(target)
}

fun main() {
    SwingUtilities.invokeAndWait {
        val config = WaveformConfigService()
        config.state.windowSeconds = 20.0
        val connection = fake<MessageBusConnection> { _, _, _ -> null }
        val bus = fake<MessageBus> { _, method, _ -> if (method.name == "connect") connection else null }
        val session = fake<XDebugSession> { _, method, _ ->
            when (method.name) {
                "isSuspended", "isStopped", "isPaused" -> false
                else -> null
            }
        }
        val manager = TestManager(session)
        val project = fake<Project> { _, method, args ->
            when (method.name) {
                "getMessageBus" -> bus
                "getService" -> when (args?.firstOrNull()) {
                    WaveformConfigService::class.java -> config
                    XDebuggerManager::class.java -> manager
                    else -> null
                }
                "isDisposed", "isDefault", "isOpen", "isInitialized" -> false
                "getName" -> "Waveform test"
                else -> null
            }
        }
        val panel = WaveformPanel(project)
        try {
            val canvas = member(panel, "plotCanvas") as PlotCanvas
            check(canvas.windowSeconds == 20.0) { "restoring configuration overwrote window duration" }
            check(member(panel, "hasActiveSession") == true) { "opening panel during Debug did not attach immediately" }
            panel.addVariableByName("gyro[0]")
            val buffer = member(panel, "dataBuffer") as DataBuffer
            val channel = buffer.getChannels().single()
            @Suppress("UNCHECKED_CAST")
            val checkboxes = member(panel, "channelCheckboxes") as Map<String,JCheckBox>
            val checkbox = checkboxes.getValue("gyro[0]")
            checkbox.doClick()
            check(!channel.visible) { "unchecking variable did not hide the actual curve" }
            val channelPanel = member(panel, "channelCheckPanel") as JPanel
            val item = channelPanel.components.single() as JPanel
            val name = item.components.filterIsInstance<JLabel>().first { it.text == "gyro[0]" }
            val click = MouseEvent(name, MouseEvent.MOUSE_CLICKED, 0, 0, 1, 1, 1, false, MouseEvent.BUTTON1)
            name.mouseListeners.forEach { it.mouseClicked(click) }
            check(checkbox.isSelected && channel.visible && member(panel, "plotRequested") == true) { "clicking variable name did not request plotting" }
            val status = member(panel, "liveStatusLabel") as JLabel
            check(status.text.contains("Pause MCU once")) { "unresolved running variable did not request initial pause: ${status.text}" }
            val spinner = member(panel, "windowSpinner") as JSpinner
            spinner.value = 5.0
            check(canvas.windowSeconds == 5.0 && config.state.windowSeconds == 5.0) { "window control did not update canvas and persistent config" }
            println("PASS: actual WaveformPanel attaches to Debug, restores settings, plots on name click, hides on uncheck, and saves window changes")
        } finally {
            panel.dispose()
        }
    }
}
