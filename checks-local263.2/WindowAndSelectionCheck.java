import com.github.waveformplotter.*;
import java.util.Map;

public class WindowAndSelectionCheck {
    public static void main(String[] args) {
        StreamingTimeWindow window = new StreamingTimeWindow();
        check(window.rangeAt(0).getStartSeconds()==-10 && window.rangeAt(0).getEndSeconds()==0, "initial streaming window");
        check(window.rangeAt(100).getStartSeconds()==90 && window.rangeAt(100).getEndSeconds()==100, "old history must scroll away");
        window.setDuration(5);
        check(window.rangeAt(100).getStartSeconds()==95, "configurable window length");
        window.panTo(40);
        check(window.rangeAt(110).getStartSeconds()==40 && !window.getFollowingLatest(), "manual history browsing");
        window.zoomBy(2, 0.5, 110);
        check(window.rangeAt(110).getStartSeconds()==37.5 && window.getDurationSeconds()==10, "zoom keeps anchor");
        window.followLatest();
        check(window.rangeAt(110).getStartSeconds()==100, "resume streaming after history browsing");
        window.zoomBy(0.5, 0.5, 120);
        check(window.rangeAt(120).getStartSeconds()==115 && window.rangeAt(120).getEndSeconds()==120, "live zoom follows latest");

        DataBuffer data = new DataBuffer();
        ChannelData x = data.addChannel("x");
        ChannelData y = data.addChannel("y");
        data.pushAll(Map.of("x", 1.0, "y", 2.0), 0);
        long version = data.getVersion();
        data.setChannelVisible("x", false);
        check(!x.getVisible() && y.getVisible() && data.getVersion()>version, "hide channel and invalidate FFT cache");
        data.pushAll(Map.of("y", 3.0), 1000000000L);
        data.setChannelVisible("x", true);
        check(x.getVisible() && Double.isNaN(x.get(1)) && y.get(1)==3.0, "re-enabled channel preserves time alignment");
        System.out.println("PASS: streaming range, zoom, history browsing, and channel visibility/alignment");
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
