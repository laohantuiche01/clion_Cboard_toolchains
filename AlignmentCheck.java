import com.github.waveformplotter.*;
import java.util.*;

public class AlignmentCheck {
    public static void main(String[] args) {
        DataBuffer buffer = new DataBuffer();
        ChannelData x = buffer.addChannel("gyro[0]");
        buffer.pushAll(Map.of("gyro[0]", 1.0), 1000000000L);
        buffer.pushAll(Map.of("gyro[0]", 2.0), 2000000000L);
        ChannelData y = buffer.addChannel("gyro[1]");
        buffer.pushAll(Map.of("gyro[0]", 3.0, "gyro[1]", 30.0), 3000000000L);
        require(y.getSize() == buffer.getTsSize(), "late channel has " + y.getSize() + " values for " + buffer.getTsSize() + " timestamps");
        require(Double.isNaN(y.get(0)) && Double.isNaN(y.get(1)), "late channel must mark old frames as missing");
        require(y.get(2) == 30.0 && x.get(2) == 3.0 && buffer.getTimeSeconds(2) == 2.0, "both channels must share the third timestamp");
        buffer.pushAll(Map.of("gyro[0]", 4.0), 4000000000L);
        require(Double.isNaN(y.get(3)), "missing sample must not shift future values");
        for (int i=5; i<10020; i++) {
            buffer.pushAll(Map.of("gyro[0]", (double)i, "gyro[1]", (double)i*10), (long)i*1000000000L);
        }
        ChannelData z = buffer.addChannel("gyro[2]");
        buffer.pushAll(Map.of("gyro[0]", 10020.0, "gyro[1]", 100200.0, "gyro[2]", 42.0), 10020000000000L);
        require(x.getSize()==buffer.getTsSize() && y.getSize()==buffer.getTsSize() && z.getSize()==buffer.getTsSize(), "ring buffers must stay aligned after wrapping");
        require(z.get(z.getSize()-1)==42.0 && Double.isNaN(z.get(0)), "late channel must align when history buffer is full");
        buffer.clearAll();
        require(buffer.getTsSize()==0 && buffer.getChannelCount()==3, "clear must keep channel configuration");
        buffer.pushAll(Map.of("gyro[1]", 7.0), 123000000000L);
        require(buffer.getTimeSeconds(0)==0.0 && Double.isNaN(x.get(0)) && y.get(0)==7.0, "new acquisition must start at zero with aligned channels");
        System.out.println("PASS: late channel, missing samples, ring wrap, and clear/restart alignment");
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
