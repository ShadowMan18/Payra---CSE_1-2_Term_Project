package codes;

import java.awt.*;

public class Screen
{
    private static final GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private static final Rectangle bounds = g.getMaximumWindowBounds();
    private static final int width = bounds.width;
    private static final int height = bounds.height;
    public static final int SCREENWIDTH = width - 5;
    public static final int SCREENHEIGHT = height - 35;
}
