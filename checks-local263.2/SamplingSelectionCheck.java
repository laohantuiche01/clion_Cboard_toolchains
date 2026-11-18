import com.github.waveformplotter.*;
import kotlin.Unit;
import java.io.*;
import java.util.*;

/** Exercises the real Telnet command/response sampler with in-memory streams. */
public class SamplingSelectionCheck {
    public static void main(String[] args) throws Exception {
        List<String> commands = new ArrayList<>();
        ArrayDeque<Byte> replies = new ArrayDeque<>();
        InputStream input = new InputStream() {
            @Override public int available() { return 0; }
            @Override public int read() { return replies.isEmpty() ? -1 : replies.removeFirst() & 255; }
            @Override public int read(byte[] bytes, int offset, int length) {
                if (replies.isEmpty()) return -1;
                int n=0;
                while (n<length && !replies.isEmpty()) bytes[offset+n++]=replies.removeFirst();
                return n;
            }
        };
        Writer output = new Writer() {
            final StringBuilder pending = new StringBuilder();
            @Override public void write(char[] bytes, int off, int len) { pending.append(bytes, off, len); }
            @Override public void close() {}
            @Override public void flush() {
                String text=pending.toString(); pending.setLength(0);
                for (String line:text.split("\\R")) {
                    if (line.isBlank()) continue;
                    commands.add(line);
                    boolean y=line.contains("0x20000004");
                    String response=(y?"0x20000004: 40000000":"0x20000000: 3f800000")+"\n> ";
                    for (byte b:response.getBytes(java.nio.charset.StandardCharsets.US_ASCII)) replies.addLast(b);
                }
            }
        };
        DataBuffer data = new DataBuffer(); data.addChannel("x"); data.addChannel("y");
        LiveWatchService service = new LiveWatchService(data, () -> Unit.INSTANCE);
        field(service, "telnetInput", input);
        field(service, "telnetWriter", new PrintWriter(output));
        var entriesField = LiveWatchService.class.getDeclaredField("watchEntries"); entriesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String,LiveWatchService.WatchEntry> entries = (Map<String,LiveWatchService.WatchEntry>) entriesField.get(service);
        entries.put("x", new LiveWatchService.WatchEntry("x", 0x20000000L, LiveWatchService.DataType.FLOAT, 4));
        entries.put("y", new LiveWatchService.WatchEntry("y", 0x20000004L, LiveWatchService.DataType.FLOAT, 4));
        var sample=LiveWatchService.class.getDeclaredMethod("sample"); sample.setAccessible(true);
        service.setTrackedVariables(List.of("x"));
        sample.invoke(service); sample.invoke(service);
        check(commands.size()==2 && commands.stream().allMatch(c->c.equals("mdw 0x20000000 1")), "unselected variable was sampled");
        service.setTrackedVariables(List.of("y"));
        sample.invoke(service);
        check(commands.get(2).equals("mdw 0x20000004 1"), "selection change not applied to actual Telnet command");
        service.removeVariable("x"); data.removeChannel("x");
        sample.invoke(service);
        check(data.getChannels().stream().noneMatch(c->c.getName().equals("x")), "removed channel was recreated");
        ChannelData y=data.getChannels().get(0);
        check(y.getSize()==data.getTsSize() && y.get(y.getSize()-1)==2.0 && Double.isNaN(y.get(0)), "selected value or timestamp alignment wrong");
        check(service.getSampleCount()==4, "sampler stalled during selection change");
        System.out.println("PASS: real Telnet sampler switches selected variables, parses float replies, and keeps removed curves absent (in-memory I/O)");
    }
    static void field(Object target, String name, Object value) throws Exception {
        var f=target.getClass().getDeclaredField(name); f.setAccessible(true); f.set(target,value);
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
