import com.github.waveformplotter.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Map;

public class StreamingCheck {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DataBuffer buffer = new DataBuffer();
            buffer.addChannel("x");
            for (int i=0; i<=100; i++) buffer.pushAll(Map.of("x", (double)i), i*1000000000L);
            PlotCanvas canvas = new PlotCanvas(buffer);
            try {
                canvas.setSize(1000, 400);
                var image = new BufferedImage(1000, 400, BufferedImage.TYPE_INT_RGB);
                var graphics = image.createGraphics();
                canvas.paint(graphics);
                graphics.dispose();
                Field scale = PlotCanvas.class.getDeclaredField("xScaleSPP");
                Field offset = PlotCanvas.class.getDeclaredField("xOffsetSec");
                scale.setAccessible(true);
                offset.setAccessible(true);
                double duration = scale.getDouble(canvas) * (1000-62-12);
                require(Math.abs(duration-10.0)<1e-8, "100 seconds of history should still display a 10-second window, got " + duration);
                require(Math.abs(offset.getDouble(canvas)-90.0)<1e-8, "visible start should follow latest sample at 90 seconds");
                buffer.addChannel("hidden");
                buffer.pushAll(Map.of("x", 101.0, "hidden", 1000000.0), 101000000000L);
                buffer.setChannelVisible("hidden", false);
                var next = image.createGraphics(); canvas.paint(next); next.dispose();
                Field max = PlotCanvas.class.getDeclaredField("currentYMax"); max.setAccessible(true);
                require(max.getDouble(canvas)<1000, "hidden curve must not affect chart autoscaling");
                buffer.setChannelVisible("hidden", true);
                next = image.createGraphics(); canvas.paint(next); next.dispose();
                require(max.getDouble(canvas)>100000, "selected curve must appear in chart autoscaling");
                canvas.setWindowSeconds(5);
                next = image.createGraphics(); canvas.paint(next); next.dispose();
                require(Math.abs(scale.getDouble(canvas)*(1000-62-12)-5)<1e-8, "window control must change actual rendered range");
                System.out.println("PASS: actual PlotCanvas streams the latest window, resizes it, and includes only visible curves");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            } finally {
                canvas.dispose();
            }
        });
    }
    static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
