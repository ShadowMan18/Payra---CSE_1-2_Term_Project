package com.example.javafx_project;

import java.awt.*;

public class Screen
{
    private static final GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private static final Rectangle bounds = g.getMaximumWindowBounds();
    private static final double width = bounds.width;
    private static final double height = bounds.height;
    public static double getWidth()
    {
        return width;
    }
    public static double getHeight()
    {
        return height;
    }
}
