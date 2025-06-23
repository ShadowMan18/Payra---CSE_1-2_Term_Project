package com.example.javafx_project;

import java.awt.*;

public class Screen
{
    private static final GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private static final Rectangle bounds = g.getMaximumWindowBounds();
    private static final int width = bounds.width;
    private static final int height = bounds.height;
    public static int getWidth()
    {
        return width;
    }
    public static int getHeight()
    {
        return height;
    }
}
