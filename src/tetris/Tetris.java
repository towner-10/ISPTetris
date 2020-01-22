package tetris;
 
import java.awt.event.*;
import static java.lang.Math.*;
import static java.lang.String.format;
import java.util.*;
import static tetris.Config.*;
import hsa_ufa.Console;

/*
Description: Tetris with rules from the competitive Tetris game.

Author: Collin Town

Project: This project demonstrates my knowledge of the course material and my own learning. I show my knowledge of object-oriented programming,
variables, if-else statements, for-loops, threads for more performant code.
*/

public class Tetris implements Runnable {
    
    // Control the direction
    enum Dir {
        right(1, 0), down(0, 1), left(-1, 0);
 
        Dir(int x, int y) {
            this.x = x;
            this.y = y;
        }

        final int x;
        final int y;
    };
 
    public static final int EMPTY = -1;
    public static final int BORDER = -2;
 
    Shape fallingShape;
    Shape nextShape;

    Console c;
 
    // position of falling shape
    int fallingShapeRow;
    int fallingShapeCol;

    // Fast dropping the shape
    boolean fastDrop = false;
 
    final int[][] grid = new int[nRows][nCols];
 
    // https://www.geeksforgeeks.org/multithreading-in-java/
    Thread fallingThread;
    final Scoreboard scoreboard = new Scoreboard();
    static final Random rand = new Random();
 
    public Tetris() {
        c = new Console(dim.width, dim.height, "Tetris");
        c.setBackgroundColor(bgColour);

        initGrid();
        selectShape();

        startNewGame();
        
        /*
        I wanted to add a better way of detecting input in this program so I decided to use the KeyListener
        https://docs.oracle.com/javase/7/docs/api/java/awt/event/KeyListener.html
        I am basically declaring a class within a class here. I override the keyPressed method to add my custom code.
        */
        c.addKeyListener(new KeyAdapter() {
            boolean fastDown;
 
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        if (canRotate(fallingShape))
                            rotate(fallingShape);
                        break;
 
                    case KeyEvent.VK_LEFT:
                        if (canMove(fallingShape, Dir.left))
                            move(Dir.left);
                        break;
 
                    case KeyEvent.VK_RIGHT:
                        if (canMove(fallingShape, Dir.right))
                            move(Dir.right);
                        break;
 
                    case KeyEvent.VK_DOWN:
                        fastDrop = true;
                        break;

                    case KeyEvent.VK_SPACE:
                        if (fastDown == false && scoreboard.isGameOver() == false) {
                            fastDown = true;

                            while (canMove(fallingShape, Dir.down)) {
                                move(Dir.down);
                                draw();
                            }

                            shapeHasLanded();
                        }
                        if (scoreboard.isGameOver() == true) {
                            startNewGame();
                            draw();
                        }
                        break;
                }
                draw();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        fastDrop = false;
                        break;

                    case KeyEvent.VK_SPACE:
                        fastDown = false;
                        break;
                }
            }
        });
    }
 
    void selectShape() {
        fallingShapeRow = 1;
        fallingShapeCol = 5;
        fallingShape = nextShape;
        Shape[] shapes = Shape.values();
        nextShape = shapes[rand.nextInt(shapes.length)];
        if (fallingShape != null)
            fallingShape.reset();
    }
 
    void startNewGame() {
        initGrid();
        selectShape();
        scoreboard.reset();
        (fallingThread = new Thread(this)).start();
    }
 
    void stop() {
        if (fallingThread != null) {
            Thread tmp = fallingThread;
            fallingThread = null;
            tmp.interrupt();
        }
    }

    void printGrid() {
        for (int r = 0; r < nRows; r++) {
            for (int c = 0; c < nCols; c++) {
                System.out.print(grid[r][c] + ", ");
            }
            System.out.println();
        }
    }
 
    void initGrid() {
        for (int r = 0; r < nRows; r++) {
            Arrays.fill(grid[r], EMPTY);
            for (int c = 0; c < nCols; c++) {
                if (c == 0 || c == nCols - 1 || r == nRows - 1)
                    grid[r][c] = BORDER;
            }
        }
    }

    int currentSpeed() {
        if (fastDrop == true) {
            return fastDropSpeed;
        }
        return scoreboard.getSpeed();
    }
 
    // This method is like a while true loop that runs automatically when you have a thread running
    @Override
    public void run() {
        while (Thread.currentThread() == fallingThread) {
            try {
                Thread.sleep(currentSpeed());
            } catch (InterruptedException e) {
                return;
            }
 
            if (!scoreboard.isGameOver()) {
                if (canMove(fallingShape, Dir.down)) {
                    move(Dir.down);
                } else {
                    shapeHasLanded();
                }
                draw();
            }
        }
    }
 
    void drawStartScreen() {
        c.setFont(mainFont);
 
        c.setColor(titlebgColour);
        c.fillRect(titleRect.x, titleRect.y, titleRect.width, titleRect.height);
        c.fillRect(clickRect.x, clickRect.y, clickRect.width, clickRect.height);
 
        c.setColor(textColour);
        c.drawString("Tetris", titleX, titleY);
 
        c.setFont(smallFont);
        c.drawString("Press Space to Start", startX, startY);
    }
 
    void drawSquare(int colorIndex, int r, int i, boolean preview) {
        c.setColor(colours[colorIndex]);

        if (preview == true) 
            c.fillRect(previewCenterX + i * blockSize, previewCenterY + r * blockSize, blockSize, blockSize);
        else
            c.fillRect(leftMargin + i * blockSize, topMargin + r * blockSize, blockSize, blockSize);

        c.setColor(squareBorder);
        
        if (preview == true)
            c.drawRect(previewCenterX + i * blockSize, previewCenterY + r * blockSize, blockSize, blockSize);
        else
            c.drawRect(leftMargin + i * blockSize, topMargin + r * blockSize, blockSize, blockSize);
    }
 
    void drawUI() {
        // grid background
        c.setColor(gridColour);
        c.fillRect(gridRect.x, gridRect.y, gridRect.width, gridRect.height);

        c.setColor(gridBorderColour);
        for (int i = 0; i < nRows - 1; i++) {
            for (int j = 1; j < nCols - 1; j++) {
                c.drawRect(leftMargin + j * blockSize, topMargin + i * blockSize, blockSize, blockSize);
            }
        }
 
        // the blocks dropped in the grid
        for (int r = 0; r < nRows; r++) {
            for (int c = 0; c < nCols; c++) {
                int idx = grid[r][c];
                if (idx > EMPTY)
                    drawSquare(idx, r, c, false);
            }
        }
 
        // the borders of grid and preview panel
        c.setColor(gridBorderColour);
        c.drawRect(gridRect.x, gridRect.y, gridRect.width, gridRect.height);
        c.drawRect(previewRect.x, previewRect.y, previewRect.width, previewRect.height);
 
        // scoreboard
        int x = scoreX;
        int y = scoreY;
        
        c.setColor(textColour);
        c.setFont(smallFont);
        c.drawString(format("Highscore  %6d", scoreboard.getTopscore()), x, y);
        c.drawString(format("Level      %6d", scoreboard.getLevel()), x, y + 30);
        c.drawString(format("Lines      %6d", scoreboard.getLines()), x, y + 60);
        c.drawString(format("Score      %6d", scoreboard.getScore()), x, y + 90);
 
        // preview
        int minX = 5, minY = 5, maxX = 0, maxY = 0;
        for (int[] p : nextShape.pos) {
            minX = min(minX, p[0]);
            minY = min(minY, p[1]);
            maxX = max(maxX, p[0]);
            maxY = max(maxY, p[1]);
        }

        for (int[] p : nextShape.shape) {
            drawSquare(nextShape.ordinal(), p[1], p[0], true);
        }
    }
 
    void drawFallingShape() {
        int idx = fallingShape.ordinal();
        for (int[] p : fallingShape.pos)
            drawSquare(idx, fallingShapeRow + p[1], fallingShapeCol + p[0], false);
    }

    void draw() {
        c.clear();

        drawUI();

        if (scoreboard.isGameOver()) {
            drawStartScreen();
        } else {
            drawFallingShape();
        }
    }
 
    boolean canRotate(Shape s) {
        if (s == Shape.Square)
            return false;
 
        int[][] pos = new int[4][2];
        for (int i = 0; i < pos.length; i++) {
            pos[i] = s.pos[i].clone();
        }
 
        for (int[] row : pos) {
            int tmp = row[0];
            row[0] = row[1];
            row[1] = -tmp;
        }
 
        for (int[] p : pos) {
            int newCol = fallingShapeCol + p[0];
            int newRow = fallingShapeRow + p[1];
            if (grid[newRow][newCol] != EMPTY) {
                return false;
            }
        }
        return true;
    }
 
    void rotate(Shape s) {
        if (s == Shape.Square)
            return;
 
        for (int[] row : s.pos) {
            int tmp = row[0];
            row[0] = row[1];
            row[1] = -tmp;
        }
    }
 
    void move(Dir dir) {
        fallingShapeRow += dir.y;
        fallingShapeCol += dir.x;
    }
 
    boolean canMove(Shape s, Dir dir) {
        for (int[] p : s.pos) {
            int newCol = fallingShapeCol + dir.x + p[0];
            int newRow = fallingShapeRow + dir.y + p[1];
            if (grid[newRow][newCol] != EMPTY)
                return false;
        }
        return true;
    }
 
    void shapeHasLanded() {
        addShape(fallingShape);
        if (fallingShapeRow < 2) {
            scoreboard.setGameOver();
            scoreboard.setTopscore();
            stop();
        } else {
            scoreboard.addLines(removeLines());
        }
        selectShape();
    }
 
    int removeLines() {
        int count = 0;
        for (int r = 0; r < nRows - 1; r++) {
            for (int c = 1; c < nCols - 1; c++) {
                if (grid[r][c] == EMPTY)
                    break;
                if (c == nCols - 2) {
                    count++;
                    removeLine(r);
                }
            }
        }
        return count;
    }
 
    void removeLine(int line) {
        for (int c = 0; c < nCols; c++)
            grid[line][c] = EMPTY;
 
        for (int c = 0; c < nCols; c++) {
            for (int r = line; r > 0; r--)
                grid[r][c] = grid[r - 1][c];
        }
    }
 
    void addShape(Shape s) {
        for (int[] p : s.pos)
            grid[fallingShapeRow + p[1]][fallingShapeCol + p[0]] = s.ordinal();
    }
 
    public static void main(String[] args) {
        Tetris t = new Tetris();
    }
}
