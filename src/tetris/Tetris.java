package tetris;
 
import java.awt.event.*;
import static java.lang.Math.*;
import static java.lang.String.format;
import java.util.*;
import static tetris.Config.*;
import hsa_ufa.Console;
 
public class Tetris implements Runnable {
    enum Dir {
        right(1, 0), down(0, 1), left(-1, 0);
 
        Dir(int x, int y) {
            this.x = x;
            this.y = y;
        }
        final int x, y;
    };
 
    public static final int EMPTY = -1;
    public static final int BORDER = -2;
 
    Shape fallingShape;
    Shape nextShape;

    Console c;
 
    // position of falling shape
    int fallingShapeRow;
    int fallingShapeCol;

    boolean fastDrop = false;
 
    final int[][] grid = new int[nRows][nCols];
 
    Thread fallingThread;
    final Scoreboard scoreboard = new Scoreboard();
    static final Random rand = new Random();
 
    public Tetris() {
        c = new Console(dim.width, dim.height, "Tetris");
        c.setBackground(bgColor);

        initGrid();
        selectShape();

        startNewGame();
 
        c.addKeyListener(new KeyAdapter() {
            boolean fastDown;
 
            @Override
            public void keyPressed(KeyEvent e) {
 
                if (scoreboard.isGameOver())
                    return;
 
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
                        if (!fastDown) {
                            fastDown = true;

                            while (canMove(fallingShape, Dir.down)) {
                                move(Dir.down);
                                draw();
                            }

                            shapeHasLanded();
                        }
                        break;

                    case KeyEvent.VK_SPACE:
                        if (scoreboard.isGameOver()) {
                            startNewGame();
                            draw();
                        }
                        else {
                            fastDrop = true;
                        }
                        break;
                }
                draw();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        fastDrop = false;
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
        stop();
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
            return 75;
        }
        return scoreboard.getSpeed();
    }
 
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
 
        c.setColor(titlebgColor);
        c.fillRect(titleRect.x, titleRect.y, titleRect.width, titleRect.height);
        c.fillRect(clickRect.x, clickRect.y, clickRect.width, clickRect.height);
 
        c.setColor(textColor);
        c.drawString("Tetris", titleX, titleY);
 
        c.setFont(smallFont);
        c.drawString("Press Space to Start", startX, startY);
    }
 
    void drawSquare(int colorIndex, int r, int i) {
        c.setColor(colors[colorIndex]);
        c.fillRect(leftMargin + i * blockSize, topMargin + r * blockSize, blockSize, blockSize);

        c.setColor(squareBorder);
        
        c.drawRect(leftMargin + i * blockSize, topMargin + r * blockSize, blockSize, blockSize);
    }
 
    void drawUI() {
        // grid background
        c.setColor(gridColor);
        c.fillRect(gridRect.x, gridRect.y, gridRect.width, gridRect.height);
 
        // the blocks dropped in the grid
        for (int r = 0; r < nRows; r++) {
            for (int c = 0; c < nCols; c++) {
                int idx = grid[r][c];
                if (idx > EMPTY)
                    drawSquare(idx, r, c);
            }
        }
 
        // the borders of grid and preview panel
        c.setColor(gridBorderColor);
        c.drawRect(gridRect.x, gridRect.y, gridRect.width, gridRect.height);
        c.drawRect(previewRect.x, previewRect.y, previewRect.width, previewRect.height);
 
        // scoreboard
        int x = scoreX;
        int y = scoreY;
        c.setColor(textColor);
        c.setFont(smallFont);
        c.drawString(format("highscore  %6d", scoreboard.getTopscore()), x, y);
        c.drawString(format("level      %6d", scoreboard.getLevel()), x, y + 30);
        c.drawString(format("lines      %6d", scoreboard.getLines()), x, y + 60);
        c.drawString(format("score      %6d", scoreboard.getScore()), x, y + 90);
 
        // preview
        int minX = 5, minY = 5, maxX = 0, maxY = 0;
        for (int[] p : nextShape.pos) {
            minX = min(minX, p[0]);
            minY = min(minY, p[1]);
            maxX = max(maxX, p[0]);
            maxY = max(maxY, p[1]);
        }
        double cx = previewCenterX - ((minX + maxX + 1) / 2.0 * blockSize);
        double cy = previewCenterY - ((minY + maxY + 1) / 2.0 * blockSize);
 
        //g.translate(cx, cy);
        for (int[] p : nextShape.shape)
            drawSquare(nextShape.ordinal(), p[1], p[0]);
        //g.translate(-cx, -cy);
    }
 
    void drawFallingShape() {
        int idx = fallingShape.ordinal();
        for (int[] p : fallingShape.pos)
            drawSquare(idx, fallingShapeRow + p[1], fallingShapeCol + p[0]);
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
