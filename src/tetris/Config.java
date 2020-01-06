package tetris;
 
import java.awt.*;
 
final class Config {
    public enum UIColourTypes {
        SQUARE_BORDER,
        TITLE_BG,
        TEXT,
        BG,
        GRID,
        GRID_BORDER
    }

    final static Color[] colours = {Color.green, Color.red, Color.blue,
        Color.pink, Color.orange, Color.cyan, Color.magenta};
 
    final static Font mainFont = new Font("Monospaced", Font.BOLD, 48);
    final static Font smallFont = mainFont.deriveFont(Font.BOLD, 18);
 
    final static Dimension dim = new Dimension(640, 640);
 
    final static Rectangle gridRect = new Rectangle(46, 47, 308, 517);
    final static Rectangle previewRect = new Rectangle(387, 47, 200, 200);
    final static Rectangle titleRect = new Rectangle(100, 85, 252, 100);
    final static Rectangle clickRect = new Rectangle(100, 375, 252, 40);
     
    final static int blockSize = 30;
    final static int nRows = 18;
    final static int nCols = 12;
    final static int topMargin = 50;
    final static int leftMargin = 20;
    final static int scoreX = 400;
    final static int scoreY = 330;
    final static int titleX = 130;
    final static int titleY = 150;
    final static int startX = 120;
    final static int startY = 400;
    final static int previewCenterX = 467;
    final static int previewCenterY = 97;

    final static int fastDropSpeed = 50;
 
    final static Color squareBorder = Color.white;
    final static Color titlebgColour = Color.black;
    final static Color textColour = Color.white;
    final static Color bgColour = new Color(0x121212);
    final static Color gridColour = new Color(0x212121);
    final static Color gridBorderColour = Color.white;
}