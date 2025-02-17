package com.example.mdp_group_14;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;


public class GridMap extends View {
    // each cell is 10cm x 10cm
    final int CELL_UNIT_SIZE_CM = 10;
    boolean needsRedraw = false;


    // Cell class is configure the cells to be generated on the XML mapView
    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        String type;
        int id = -1;

        private Cell(float startX, float startY, float endX, float endY, Paint paint, String type) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
            this.type = type;
        }

        public void setType(String type) {
            this.type = type;
            switch (type) {
                case "image":
                    this.paint = imageColor;
                    break;
                case "obstacle":
                    this.paint = obstacleColor;
                    break;
                case "robot":
                    this.paint = robotColor;
                    break;
                // this is for where the robot is facing
                case "robotfront":
                    this.paint = robotFrontColor;
                    break;
                case "end":
                    this.paint = endColor;
                    break;
                case "start":
                    this.paint = startColor;
                    break;
                case "waypoint":
                    this.paint = waypointColor;
                    break;
                case "unexplored":
                    this.paint = unexploredColor;
                    break;
                case "explored":
                    this.paint = exploredColor;
                    break;
                case "arrow":
                    this.paint = arrowColor;
                    break;
                case "fastestPath":
                    this.paint = fastestPathColor;
                    break;
                default:
                    showLog("setType default: " + type);
                    break;
            }
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }

    SharedPreferences sharedPreferences;

    private final Paint blackPaint = new Paint();
    private final Paint whitePaint = new Paint();
    private final Paint maroonPaint = new Paint();
    private final Paint obstacleColor = new Paint();
    private final Paint imageColor = new Paint();
    private final Paint robotColor = new Paint();
    private final Paint robotFrontColor = new Paint();
    private final Paint endColor = new Paint();
    private final Paint startColor = new Paint();
    private final Paint waypointColor = new Paint();
    private final Paint unexploredColor = new Paint();
    private final Paint exploredColor = new Paint();
    private final Paint arrowColor = new Paint();
    private final Paint fastestPathColor = new Paint();

    private static String robotDirection = "None";
    private static int[] startCoord = new int[]{-1, -1};
    private static int[] curCoord = new int[]{-1, -1};
    private static int[] oldCoord = new int[]{-1, -1};
    // obstacleCoord stores a list of all the different obstacles' coordinates
    private static ArrayList<int[]> obstacleCoord = new ArrayList<>();

    // controls whether or not the robot is to be re-drawn upon calling onDraw() after an invalidate()
    // SHOULD be 'true' upon selecting a start position using SET START POINT btn, and back to false if robot goes out of bounds
    public static boolean canDrawRobot = false;
    // true when "set start point" button is clicked
    // if true, then cur coordinate can be edited to be set to start point
    private static boolean startCoordStatus = false;
    private static boolean setObstacleStatus = false;
    private static final boolean unSetCellStatus = false;
    private static final boolean setExploredStatus = false;
    private static boolean validPosition = false;
    private static final String TAG = "GridMap";
    private static final int COL = 20;
    private static final int ROW = 20;
    // cell size is calculated by taking the canvas width / (cols+1)
    // why cols+1? because although we want a 20x20 grid, we need a 21x21 grid
    // so that we can store the grid labels, in the 1st col cells[0][?] and last row cells[?][20]
    private static float cellSize;

    // cells is a 2d array of the cells, its initialised via createCell() in onDraw()
    // its used to draw the custom grid on the view, in this case its a 21x21 grid, to account for the grid labels as well, it starts from top left cell
    // however weird thing is its accessed cells[col][row]
    private static Cell[][] cells;
    Map<String, String> val2IdxMap;

    private boolean mapDrawn = false;



    // UNLIKE the cells grid which is 21x21 and used for the view
    // displayedImageIDs and displayedImageBearings on the other hand are 20x20 arrays, that reflects the UI's displayed grid (which is in cartesian form)
    // so ITEM_LIST.get(0)[0] for example, would represent the origin, row#=1, col#=1

    // displayedImageIDs, grid that stores if obstacle has been scanned
    //    Non-Static: Belongs to an instance of the class.
        //    Each object created will be its own entity

        //    each new String[20] creates an array of 20 string elements (this is the row)
        //    Arrays.asList() takes in these rows to make a 2d grid, i.e. a List<String[]>
        //    new ArrayList<>(Arrays.asList(...)) makes the list dynamic, you can add, remove, or modify rows (String[])
    public ArrayList<String[]> displayedImageIDs = new ArrayList<>(Arrays.asList(
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20]
    ));

    // displayedImageBearings, stores all obstacles' directions as a string, "North" e.g.
    //    Static: Belongs to the class itself, shared across all instances.
        //    There is only one shared copy of this list for all instances of the class.
    public static ArrayList<String[]> displayedImageBearings = new ArrayList<>(Arrays.asList(
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20]
    ));

    static ClipData clipData;
    static Object localState;
    int initialColumn, initialRow;
    int endColumn, endRow;
    String oldItem = "";


    /**
     * Constructor for initializing the custom GridMap view from XML.
     *
     * @param context The Context in which the view is being created. It provides access to
     *                system resources and application-specific information.
     * @param attrs   The AttributeSet contains the set of attributes provided in the XML layout file.
     *                These attributes are used to initialize the view's custom properties (e.g., colors, styles).
     *
     * The constructor configures custom paint objects for drawing on the grid, initializes shared preferences
     * for storing and retrieving data, and sets up the map's internal state (like val2IdxMap).
     * It also ensures that the view allows custom drawing by calling `allowCustomMap()`.
     */
    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        allowCustomMap();
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        whitePaint.setColor(Color.WHITE);
        whitePaint.setTextSize(17);
        whitePaint.setTextAlign(Paint.Align.CENTER);
        maroonPaint.setColor(getResources().getColor(R.color.brightRed));
        maroonPaint.setStrokeWidth(8);
        obstacleColor.setColor(getResources().getColor(R.color.black));
        imageColor.setColor(getResources().getColor(R.color.rockColor));
        robotColor.setColor(getResources().getColor(R.color.pikaYellow));
        robotColor.setStrokeWidth(2);
        robotFrontColor.setColor(Color.BLUE);
        endColor.setColor(Color.RED);
        startColor.setColor(Color.CYAN);
        waypointColor.setColor(Color.GREEN);
        unexploredColor.setColor(getResources().getColor(R.color.skyBlue));
        exploredColor.setColor(getResources().getColor(R.color.lighterYellow));
        arrowColor.setColor(Color.BLACK);
        fastestPathColor.setColor(Color.MAGENTA);
        Paint newpaint = new Paint();
        newpaint.setColor(Color.TRANSPARENT);

        // get shared preferences
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences",
                Context.MODE_PRIVATE);

        this.val2IdxMap = new HashMap<>();
    }

    // tells android to allow custom drawing of the grid for our mapView
    private void allowCustomMap() {
        setWillNotDraw(false);
    }

    // onDraw cannot call any this.invalidate()! otherwise infinite loop, calls onDraw() again
    @Override
    protected void onDraw(Canvas canvas) {
        showLog("Entering onDraw");
        showLog("canDrawRobot = " + getCanDrawRobot());
        super.onDraw(canvas);
        showLog("Redrawing map");

        // Create cell coords
        Log.d(TAG, "Creating Cell");

        // mapDrawn boolean ensures we only run the cell creation once
        // calls createCell()
        // which creates the 2d array called "cells"
        if (!mapDrawn) {
            mapDrawn = true;
            this.createCell();
        }

        // Calls methods to draw individual cells, grid lines, axis numbers, obstacles, and the robot if needed.
        drawIndividualCell(canvas);
        drawHorizontalLines(canvas);
        drawVerticalLines(canvas);
        // adds axis numbers (0 to 19)
        drawGridNumber(canvas);
        // canDrawRobot = false on initialisation
        // curCoord is default to (-1,-1)
        if (getCanDrawRobot())
            drawRobot(canvas, curCoord);
        drawObstacles(canvas);

        showLog("Exiting onDraw");
    }

    // this func initialises the 2d cells array
    // its called before any other func like adding axis numbers or obstacles.
    private void createCell() {
        showLog("Entering cellCreate");
        // rows+1, cols+1 so that we have space to put the grid labels, e.g. 20x20 will be 21x21
        cells = new Cell[COL + 1][ROW + 1];
        this.calculateDimension();
        cellSize = this.getCellSize();

        // the other functions will just overlap the 2d array later, so just iterate every cell first
        for (int x = 0; x <= COL; x++)
            for (int y = 0; y <= ROW; y++)
                // for every cell in the 2d array cells, create Cell obj
                // Cell() takes in 2 points, a top left, and bottom right
                // (cellSize / 30) offsets every cell slightly to the right and slightly down, to give space for boundary line
                // imagine every cell as a blue box with a white outline (thickness cellsize/30) only on the left and top of the box, this essentially outlines all the grids when combined tgt
                cells[x][y] = new Cell(
                        x * cellSize + (cellSize / 30),
                        y * cellSize + (cellSize / 30),
                        (x + 1) * cellSize,
                        (y + 1) * cellSize,
                        unexploredColor,
                        "unexplored"
                );
        showLog("Exiting createCell");
    }

   // iterate all cols except 1st col (x=0)
    // iterate all rows except last row (y=20)
    // to now draw out the cells in blue
    private void drawIndividualCell(Canvas canvas) {
        showLog("Entering drawIndividualCell");
        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++)
                {
                    canvas.drawRect(
                            cells[x][y].startX,
                            cells[x][y].startY,
                            cells[x][y].endX,
                            cells[x][y].endY,
                            cells[x][y].paint
                    );
                }
        showLog("Exiting drawIndividualCell");
    }

    // recall for each cell, we made it offset to the right and down by + cellSize / 30 to make space for the boundary line, so the top and left essentially has space
    // x,y are the indices, which range from 0 to 20, to access all 21 cells in the 2d grid "cells"
    // so now we can draw the lines:
    // drawLine takes in coordinates of 2 points to draw a line b/w them, start=(x1,y1) and end=(x2,y2)
    // recall topleft = (startX, startY) and bottomright = (endX, endY)
    // here we draw a horizontal line, on y-coordinate of the topleft, while x-coordinate ranges from 2nd cell to 21st cell, i.e. cells[1][?].startX to cells[20][y].endX
    private void drawHorizontalLines(Canvas canvas) {
        for (int y = 0; y <= ROW; y++)
            canvas.drawLine(
                    cells[1][y].startX,
                    cells[1][y].startY,
                    cells[20][y].endX,
                    cells[20][y].startY,
                    whitePaint
            );
    }

    // draw the vertical lines for each left side of every grid except 1st column (since we + cellSize)
    private void drawVerticalLines(Canvas canvas) {
        for (int x = 0; x <= COL; x++)
            canvas.drawLine(
                    cells[x][0].startX + cellSize,
                    cells[x][0].startY ,
                    cells[x][0].startX + cellSize,
                    cells[x][19].endY,
                    whitePaint
            );
    }

    // Draw the axis numbers
    // the > 10 condition is to adjust offset for the double digit numbers, shifting it abit more to left for double digits
    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        // this 1st part writes the numbers from 0 to 19 @ y=20
        // i.e. numbers will be on the 21st row of the 2d grid
        for (int x = 1; x <= COL; x++) {
            if (x > 10)
                canvas.drawText(
                        Integer.toString(x - 1),
                        cells[x][20].startX + (cellSize / 5),
                        cells[x][20].startY + (cellSize / 1.5f),
                        blackPaint
                );
            else
                canvas.drawText(
                        Integer.toString(x - 1),
                        cells[x][20].startX + (cellSize / 3),
                        cells[x][20].startY + (cellSize / 1.5f),
                        blackPaint
                );
        }
        // this 2nd part writes the numbers from 0 to 19 @ x=0
        // i.e. numbers will be on the 1st column of the 2d grid
        for (int y = 0; y < ROW; y++) {
            if ((20 - y) > 10)
                canvas.drawText(
                        Integer.toString(ROW - 1 - y),
                        cells[0][y].startX + (cellSize / 4),
                        cells[0][y].startY + (cellSize / 1.5f),
                        blackPaint
                );
            else
                canvas.drawText(
                        Integer.toString(ROW - 1 - y),
                        cells[0][y].startX + (cellSize / 2.5f),
                        cells[0][y].startY + (cellSize / 1.5f),
                        blackPaint
                );
        }
        showLog("Exiting drawGridNumber");
    }

    private void drawRobot(Canvas canvas, int[] curCoord) {
        showLog("Entering drawRobot");
        curCoord = getCurCoord();
        int xCoordInt = curCoord[0];
        int yCoordInt = curCoord[1];
        showLog("drawing robot at coor = ("+xCoordInt+","+yCoordInt+")");

        int colNum = xCoordInt + 1;
        int rowNum = yCoordInt + 1;
        int row = convertRow(rowNum);
        int col = colNum;

        // Check boundary for a 3x3 robot grid
        if ((xCoordInt < 1 || xCoordInt > 18) || (yCoordInt < 1 || yCoordInt > 18)) {
            showLog("coordinate is out of bounds");
            return;
        }

        // revert any cells thats of robot and direction
        for (int i = 0; i < 21; i++) {
            for (int j = 0; j < 21; j++) {
                if (cells[i][j].type.equals("robot") || cells[i][j].type.equals("robotfront")) {
                    cells[i][j].setType("unexplored");
                }
            }
        }

        // set 3x3 grid to type robot
        for (int x = col - 1; x <= col+1; x++)
            for (int y = row - 1; y <= row+1; y++) {
                cells[x][y].setType("robot");
            }

        // Determine the front of the robot based on direction
        showLog("current robot direction = " + getRobotDirection().toLowerCase());
        switch (getRobotDirection().toLowerCase()) {
            case "up":
                cells[col][row - 1].setType("robotfront"); // front cell is above
                break;
            case "down":
                cells[col][row + 1].setType("robotfront"); // front cell is below
                break;
            case "left":
                cells[col - 1][row].setType("robotfront"); // front cell is to the left
                break;
            case "right":
                cells[col + 1][row].setType("robotfront"); // front cell is to the right
                break;
            default:
                showLog("Invalid direction: " + robotDirection);
        }
        showLog("Exiting drawRobot");
    }

    // draws obstacle cells whenever map refreshes
    private void drawObstacles(Canvas canvas) {
        showLog("Entering drawObstacles");
        // obstacleCoord is a list of all the obstacles
        for (int i = 0; i < obstacleCoord.size(); i++) { // for each recorded obstacle
            // get col and row (zero-indexed)
            int col = obstacleCoord.get(i)[0];
            int row = obstacleCoord.get(i)[1];
            // cells[col + 1][19 - row] is an unexplored obstacle (image not yet identified)
            if (displayedImageIDs.get(row)[col] == null || displayedImageIDs.get(row)[col].equals("")
                    || displayedImageIDs.get(row)[col].equals("Nil")) {
                showLog("drawObstacles: drawing obstacle ID");
                whitePaint.setTextSize(15);
                canvas.drawText(
                        String.valueOf(i + 1),
                        cells[col + 1][19 - row].startX + ((cells[1][1].endX - cells[1][1].startX) / 2),
                        cells[col + 1][19 - row].startY + ((cells[1][1].endY - cells[1][1].startY) / 2) + 5,
                        whitePaint
                );
            } else {    // cells[col + 1][19 - row] is an explored obstacle (image has been identified)
                showLog("drawObstacles: drawing image ID");
                whitePaint.setTextSize(17);
                canvas.drawText(
                        displayedImageIDs.get(row)[col],
                        cells[col + 1][19 - row].startX + ((cells[1][1].endX - cells[1][1].startX) / 2),
                        cells[col + 1][19 - row].startY + ((cells[1][1].endY - cells[1][1].startY) / 2) + 10,
                        whitePaint
                );
            }

            // color the face direction
            // imageBearings.get(row)[col], row and col are just zero-indexed based on the displayed grid (range is 0 - 19)
            switch (displayedImageBearings.get(row)[col]) {
                case "North":
                    canvas.drawLine(
                            cells[col + 1][19 - row].startX,
                            cells[col + 1][19 - row].startY,
                            cells[col + 1][19 - row].endX,
                            cells[col + 1][19 - row].startY,
                            maroonPaint
                    );
                    break;
                case "South":
                    canvas.drawLine(
                            cells[col + 1][19 - row].startX,
                            cells[col + 1][19 - row].startY + cellSize,
                            cells[col + 1][19 - row].endX,
                            cells[col + 1][19 - row].startY + cellSize,
                            maroonPaint
                    );
                    break;
                case "East":
                    canvas.drawLine(
                            cells[col + 1][19 - row].startX + cellSize,
                            cells[col + 1][19 - row].startY,
                            cells[col + 1][19 - row].startX + cellSize,
                            cells[col + 1][19 - row].endY,
                            maroonPaint
                    );
                    break;
                case "West":
                    canvas.drawLine(
                            cells[col + 1][19 - row].startX,
                            cells[col + 1][19 - row].startY,
                            cells[col + 1][19 - row].startX,
                            cells[col + 1][19 - row].endY,
                            maroonPaint
                    );
                    break;
            }
        }
        showLog("Exiting drawObstacles");
    }

    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    public boolean getValidPosition() {
        return validPosition;
    }

    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    public boolean getSetObstacleStatus() {
        return setObstacleStatus;
    }

    public void setStartCoordStatus(boolean status) {
        startCoordStatus = status;
    }

    private boolean getStartCoordStatus() {
        return startCoordStatus;
    }

    public boolean getCanDrawRobot() {
        return canDrawRobot;
    }


    // updates curCoord of robot when start point set
    // also sends bluetooth message of robot location & direction
    public void setStartCoord(int colNum, int rowNum) {
        showLog("Entering setStartCoord");
        String direction = getRobotDirection();
        if (direction.equals("None")) {
            direction = "up";
        }
        String dir = (direction.equals("up")) ? "NORTH" : (direction.equals("down")) ? "SOUTH" : (direction.equals("left")) ? "WEST" : "EAST";

        // convert to 0-indexed coord
        int xCoord = colNum-1;
        int yCoord = rowNum-1;

        // check if robot clash with any obstacles
        // if got obstacle with direction, i.e. displayedImageBearings or scanned displayedImageIDs
        for (int i = rowNum-2; i <= rowNum; i++) {
            for (int j = colNum-2; j <= colNum; j++) {
                if (!displayedImageIDs.get(i)[j].equals("") ||
                        !displayedImageBearings.get(i)[j].equals("")) {
                    showToast("Invalid Location: Clash with Obstacle");
                    resetRobotCoordinate();
                    showLog("Exiting setStartCoord");
                    return;
                }
            }
        }

        //if robot in range
        if ((xCoord >= 1 && xCoord <= 18) && (yCoord >= 1 && yCoord <= 18)) {
            // set curCoord of robot
            // only can edit curcoord if startCoordStatus is true i.e. "set start point" button toggled
            if (this.getStartCoordStatus()) {
                this.setCurCoord(colNum, rowNum, direction);
                startCoord[0] = xCoord;
                startCoord[1] = yCoord;
            }
            // send bluetooth msg
            Home.printMessage("ROBOT" + "," + xCoord * CELL_UNIT_SIZE_CM + CELL_UNIT_SIZE_CM/2 + "," + yCoord * CELL_UNIT_SIZE_CM + CELL_UNIT_SIZE_CM/2 + "," + dir.toUpperCase());
        } else {
            showToast("Robot Out of Grid");
            resetRobotCoordinate();
            showLog("out of grid" + startCoord[0] + "," + startCoord[1] + " " + curCoord[0] + "," + curCoord[1] );
        }
        showLog("Exiting setStartCoord");
    }

    private int[] getStartCoord() {
        return startCoord;
    }

    // updates curCoord of robot
    // also sets 3x3 grid in cells to type="robot"
    // also calls updateRobotAxis that updates display of coordinates
    public void setCurCoord(int colNum, int rowNum, String direction) {
        showLog("Entering setCurCoord");

        // convert to 0-indexed coord
        int xCoord = colNum-1;
        int yCoord = rowNum-1;

        if ((xCoord < 1 || xCoord > 18) || (yCoord < 1 || yCoord > 18)) {
            showLog("coordinate is out of bounds");
            return;
        }

        // check if robot clash with any obstacles
        // if got obstacle with direction, i.e. displayedImageBearings or scanned displayedImageIDs
        for (int i = rowNum-2; i <= rowNum; i++) {
            for (int j = colNum-2; j <= colNum; j++) {
                if (!displayedImageIDs.get(i)[j].equals("") ||
                        !displayedImageBearings.get(i)[j].equals("")) {
                    showToast("Invalid Location: Clash with Obstacle");
                    showLog("setCurCoord: Robot clashes with obstacle");
                    return;
                }
            }
        }

        curCoord[0] = xCoord;
        curCoord[1] = yCoord;
        validPosition = true;

        this.setRobotDirection(direction);
        this.updateRobotAxis(colNum, rowNum, direction);

        int row = this.convertRow(rowNum);
        int col = colNum;

        // revert any cells thats of robot and direction
        for (int i = 0; i < 21; i++) {
            for (int j = 0; j < 21; j++) {
                if (cells[i][j].type.equals("robot") || cells[i][j].type.equals("robotfront")) {
                    cells[i][j].setType("unexplored");
                }
            }
        }
        // set 3x3 grid to type robot
        for (int x = col - 1; x <= col+1; x++)
            for (int y = row - 1; y <= row+1; y++)
                cells[x][y].setType("robot");

        // Determine the front of the robot based on direction
        switch (direction.toLowerCase()) {
            case "up":
                cells[col][row - 1].setType("robotfront"); // front cell is above
                break;
            case "down":
                cells[col][row + 1].setType("robotfront"); // front cell is below
                break;
            case "left":
                cells[col - 1][row].setType("robotfront"); // front cell is to the left
                break;
            case "right":
                cells[col + 1][row].setType("robotfront"); // front cell is to the right
                break;
            default:
                showLog("Invalid direction: " + direction);
        }
        showLog("Exiting setCurCoord");
    }

    public int[] getCurCoord() {
        return curCoord;
    }

    private void calculateDimension() {
        this.setCellSize(getWidth() / (COL + 1));
    }

    // converts to cartesian form
    private int convertRow(int rowNum) {
        return (20 - rowNum);
    }

    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    private float getCellSize() {
        return cellSize;
    }

    private void setOldRobotCoord(int oldCol, int oldRow) {
        showLog("Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        oldRow = this.convertRow(oldRow);

        if (oldRow == 0) {
            showLog("oldRow has gone out of grid.");
            return;
        }
//        for (int x = oldCol - 1; x <= oldCol; x++)
//            for (int y = oldRow - 1; y <= oldRow; y++)
//                cells[x][y].setType("explored");
        showLog("Exiting setOldRobotCoord");
    }

    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    public String getRobotDirection() {
        return robotDirection;
    }
    public void setRobotDirection(String direction) {
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        robotDirection = direction;
        editor.putString("direction", direction);
        editor.apply();

        showLog("==Set Robot Direction== to: " + direction);
        this.invalidate();
    }

    // this function helps update display of robot direction and coordinates
    // must always call in conjunction with setRobotDirection() to invalidate grid view
    public void updateRobotAxis(int colNum, int rowNum, String direction) {
        TextView xAxisTextView = ((Activity) this.getContext()).findViewById(R.id.xAxisTextView);
        TextView yAxisTextView = ((Activity) this.getContext()).findViewById(R.id.yAxisTextView);
        TextView directionAxisTextView = ((Activity) this.getContext())
                .findViewById(R.id.directionAxisTextView);

        xAxisTextView.setText(String.valueOf(colNum - 1));
        yAxisTextView.setText(String.valueOf(rowNum - 1));
        directionAxisTextView.setText(direction);
    }

    public void setObstacleCoord(int col, int row) {
        showLog("Entering setObstacleCoord");


        int[] obstacleCoord = new int[]{col - 1, row - 1};

        GridMap.obstacleCoord.add(obstacleCoord);

        row = this.convertRow(row);
        cells[col][row].setType("obstacle");
        showLog("Exiting setObstacleCoord");

        int obstacleNumber = GridMap.obstacleCoord.size();

        if (((col - 1)) >= 0 && row >= 0) {

            Home.printMessage("OBSTACLE" + "," + obstacleNumber + "," + (col - 1) * 10 + "," + (19 - row) * 10 + "," + (displayedImageBearings.get(19 - row)[col - 1]).toUpperCase() + "\n");
//            BluetoothCommunications.getMessageReceivedTextView().append(Integer.toString((col - 1))+"\n");
//            BluetoothCommunications.getMessageReceivedTextView().append(Integer.toString((19 - row))+"\n");
//            BluetoothCommunications.getMessageReceivedTextView().append((imageBearings.get(19 - row)[col - 1]).toUpperCase()+"\n");
        } else {
            showLog("out of grid");
        }
        //updateStatus(obstacleNumber + "," + (col - 1)+ "," + (19 - row) + ","  + imageBearings.get(19 - row)[col - 1]); // north east


//        Home.printMessage({"key":"hello","value":"hello"});
    }

    private ArrayList<int[]> getObstacleCoord() {
        return obstacleCoord;
    }


    private static void showLog(String message) {
        Log.d(TAG, message);
    }
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }


    // drag event to move obstacle
    @Override
    public boolean onDragEvent(DragEvent dragEvent) {
        showLog("Entering onDragEvent");
        clipData = dragEvent.getClipData();
        localState = dragEvent.getLocalState();

        String tempID, tempBearing, testID;
        endColumn = endRow = -999;
        int obstacleNumber = GridMap.obstacleCoord.size();

        int obstacleid = -1;
        showLog("dragEvent.getAction() == " + dragEvent.getAction());
        showLog("dragEvent.getResult() is " + dragEvent.getResult());
        showLog("initialColumn = " + initialColumn + ", initialRow = " + initialRow);

        // drag and drop out of gridmap
        if ((dragEvent.getAction() == DragEvent.ACTION_DRAG_ENDED)
                && (endColumn == -999 || endRow == -999) && !dragEvent.getResult()) {
            // check if 2 arrays are same, then remove
            int obstacleid3 = -1;
            for (int i = 0; i < obstacleCoord.size(); i++) {
                if (Arrays.equals(obstacleCoord.get(i), new int[]{initialColumn - 1, initialRow - 1})) {
                    obstacleCoord.remove(i);
                    obstacleid3 = i;
                }


            }
            cells[initialColumn][20 - initialRow].setType("unexplored");
            displayedImageIDs.get(initialRow - 1)[initialColumn - 1] = "";
            displayedImageBearings.get(initialRow - 1)[initialColumn - 1] = "";

            //updateStatus( obstacleNumber + "," + (initialColumn) + "," + (initialRow) + ", Bearing: " + "-1");
            if (((initialColumn - 1)) >= 0 && ((initialRow - 1)) >= 0) {
                Home.printMessage("OBSTACLE" + "," + (obstacleid3 + 1) + "," + (initialColumn) * 10 + "," + (initialRow) * 10 + "," + "-1");
            } else {
                showLog("out of grid");
            }

        }
        // drop within gridmap
        else if (dragEvent.getAction() == DragEvent.ACTION_DROP) {
            endColumn = (int) (dragEvent.getX() / cellSize);
            endRow = this.convertRow((int) (dragEvent.getY() / cellSize));

            // if the currently dragged cell is empty, do nothing
            if (displayedImageIDs.get(initialRow - 1)[initialColumn - 1].equals("")
                    && displayedImageBearings.get(initialRow - 1)[initialColumn - 1].equals("")) {
                showLog("Cell is empty");
            }

            // if dropped within mapview but outside drawn grids, remove obstacle from lists
            // drag to left side of grid
            else if (endColumn <= 0 || endRow <= 0) {
                int obstacleid2 = -1;
                for (int i = 0; i < obstacleCoord.size(); i++) {
                    if (Arrays.equals(obstacleCoord.get(i),
                            new int[]{initialColumn - 1, initialRow - 1})) {
                        obstacleCoord.remove(i);
                        obstacleid2 = i;
                    }


                }
                cells[initialColumn][20 - initialRow].setType("unexplored");
                displayedImageIDs.get(initialRow - 1)[initialColumn - 1] = "";
                displayedImageBearings.get(initialRow - 1)[initialColumn - 1] = "";


                //updateStatus( obstacleNumber + "," + (initialColumn) + "," + (initialRow) + ", Bearing: " + "-1");

                if (((initialColumn - 1)) >= 0 && ((initialRow - 1)) >= 0) {
                    Home.printMessage("OBSTACLE" + "," + (obstacleid2 + 1) + "," + (initialColumn) * 10 + "," + (initialRow) * 10 + "," + "-1");
                } else {
                    showLog("out of grid");
                }

            }
            // if dropped within gridmap, shift it to new position unless already got existing
            else if ((1 <= initialColumn && initialColumn <= 20)
                    && (1 <= initialRow && initialRow <= 20)
                    && (1 <= endColumn && endColumn <= 20)
                    && (1 <= endRow && endRow <= 20)) {
                tempID = displayedImageIDs.get(initialRow - 1)[initialColumn - 1];
                tempBearing = displayedImageBearings.get(initialRow - 1)[initialColumn - 1];

                // check if got existing obstacle at drop location
                if (!displayedImageIDs.get(endRow - 1)[endColumn - 1].equals("")
                        || !displayedImageBearings.get(endRow - 1)[endColumn - 1].equals("")) {
                    showLog("An obstacle is already at drop location");
                } else {
                    displayedImageIDs.get(initialRow - 1)[initialColumn - 1] = "";
                    displayedImageBearings.get(initialRow - 1)[initialColumn - 1] = "";
                    displayedImageIDs.get(endRow - 1)[endColumn - 1] = tempID;
                    displayedImageBearings.get(endRow - 1)[endColumn - 1] = tempBearing;
                    // update existing obstacleCoord entry that matches the original (x,y) coords with new (x',y') coords
                    for (int i = 0; i < obstacleCoord.size(); i++) {
                        if (Arrays.equals(obstacleCoord.get(i), new int[]{initialColumn - 1, initialRow - 1})) {
                            obstacleCoord.set(i, new int[]{endColumn - 1, endRow - 1});
                            obstacleid = i;
                        }
                    }
                    // set the old obstacle's position to "unexplored" and new position to either "obstacle" or "image"
                    cells[endColumn][20 - endRow].setType(cells[initialColumn][20 - initialRow].type);
                    cells[initialColumn][20 - initialRow].setType("unexplored");

                    //updateStatus(obstacleid+1+ "," + (endColumn-1) + "," + (endRow-1) + ", Bearing: " + tempBearing);

                    if (((endColumn - 1)) >= 0 && ((endRow - 1)) >= 0) {
                        Home.printMessage("OBSTACLE" + "," + (obstacleid + 1) + "," + (endColumn - 1) * 10 + "," + (endRow - 1) * 10 + "," + tempBearing.toUpperCase());
                    } else {
                        showLog("out of grid");
                    }

                }
            } else {
                showLog("Drag event failed.");
            }
        }
        this.invalidate();
        return true;
    }

    public void callInvalidate() {
        showLog("Entering call invalidate");
        this.invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        showLog("Entering onTouchEvent");

        // check if user is pressing down
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // STEP 1: convert where the user is pressing into a row number and column number
            // ===============================================================================
            // event.getX() and event.getY() are the coordinates where you press
            // if e.g. x-coordinate=150px and cellSize=50px, then colindex=150/50=3 (truncate the decimals), i.e. its the 3rd column (index = 2)

            // recall, our cells is a 21x21 2d grid, so cells[0][1] would access the top left corner cell of the 20x20 grid
            int rowIndex = (int) (event.getY() / cellSize);
            int colIndex = (int) (event.getX() / cellSize);
            // however, on the displayed map, its in cartesian plane form, so the bottom left cell should be the origin
            // do note that here we are expressing it in row and col num, not coordinates (which is 0-indexed), so the origin starts at (1,1)
            int rowNum = this.convertRow(rowIndex); // need to take 20 - the row index, to invert it from topdown to cartesian form
            int colNum = colIndex;                  // can directly take from colIndex, because recall the 1st col is used for grid numbers, so we start at 2nd col naturally

            showLog("cells row index = " + rowIndex + ", cells col index = " + colIndex);
            showLog("row # = " + rowNum + ", column # = " + colNum);

            // assign initial row and col to their respective numbers
            initialRow = rowNum;
            initialColumn = colNum;

            // setStartPointToggleBtn is the "set start point" button
            ToggleButton setStartPointToggleBtn = ((Activity) this.getContext())
                    .findViewById(R.id.startpointToggleBtn);


            // convert the row and col numbers to 0-index
            // so that we can access the displayed grid 2d array (i.e. ITEM_LIST) to extract the old item
            try {
                oldItem = displayedImageIDs.get(initialRow - 1)[initialColumn - 1];
            } catch (IndexOutOfBoundsException e) {
                System.out.println("Invalid index! Unable to extract item from displayed grid");
                e.printStackTrace();
            }

            // STEP 2: Check Status
            // see what are we doing, is it dragging/changing obstacle/changing robot/resetting  cell/or pathing robot?
            //======================================================

            // start drag for obstacles
            // displays cell sized drag shadow when obstacles are moved around in drag mode
            if (MappingFragment.dragStatus) {
                // if rowNum and colNum out of bounds, then no shadow
                if (!((1 <= colNum && colNum <= 20)
                        && (1 <= rowNum && rowNum <= 20))) {
                    return false;
                }
                // or if the displayed map 2d array contains empty strings at rowNum, colNum, then no shadow
                else if (displayedImageIDs.get(rowNum - 1)[colNum - 1].equals("")
                        && displayedImageBearings.get(rowNum - 1)[colNum - 1].equals("")) {
                    return false;
                }
                // otherwise display a shadow
                View.DragShadowBuilder dragShadowBuilder = new MyDragShadowBuilder(this);
                this.startDrag(null, dragShadowBuilder, null, 0);
            }

            // start edit obstacle's bearing
            if (MappingFragment.changeObstacleStatus) {
                // if rowNum and colNum out of bounds, then cannot edit obstacle
                if (!((1 <= initialColumn && initialColumn <= 20)
                        && (1 <= initialRow && initialRow <= 20))) {
                    return false;
                }
                // or if the displayed map 2d array contains empty strings at rowNum, colNum, then cannot edit obstacle
                else if (displayedImageIDs.get(rowNum - 1)[colNum - 1].equals("")
                        && displayedImageBearings.get(rowNum - 1)[colNum - 1].equals("")) {
                    return false;
                }
                // otherwise allowed to edit obstacle's direction
                else {
                    showLog("Enter change obstacle status");
                    // allow user to change the obstacle's direction/bearing
                    String imageId = displayedImageIDs.get(rowNum - 1)[colNum - 1];
                    String imageBearing = displayedImageBearings.get(rowNum - 1)[colNum - 1];
                    final int tRow = rowNum;
                    final int tCol = colNum;

                    // appear popup dialog to edit direction of obstacle
                    AlertDialog.Builder mBuilder = new AlertDialog.Builder(this.getContext());
                    View mView = ((Activity) this.getContext()).getLayoutInflater()
                            .inflate(R.layout.activity_dialog,
                                    null);
                    mBuilder.setTitle("Change Existing Bearing");
                    // mBearingSpinner is the direction dropdown, in the popup dialog
                    final Spinner mBearingSpinner = mView.findViewById(R.id.bearingSpinner2);


                    // add the options for dropdown from "arrays.xml", imageID_array & imageBearing_array
                    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                            this.getContext(), R.array.imageID_array,
                            android.R.layout.simple_spinner_item);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                            this.getContext(), R.array.imageBearing_array,
                            android.R.layout.simple_spinner_item);
                    adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    // only bind adapter2 to the dropdown, adapter is ignored
                    mBearingSpinner.setAdapter(adapter2);

                    // set the dropdown to current obstacle direction first
                    switch (imageBearing) {
                        case "North":
                            mBearingSpinner.setSelection(0);
                            break;
                        case "South":
                            mBearingSpinner.setSelection(1);
                            break;
                        case "East":
                            mBearingSpinner.setSelection(2);
                            break;
                        case "West":
                            mBearingSpinner.setSelection(3);
                    }

                    // Update direction change when they click "OK"
                    mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // update displayedImageBearings with the new direction set
                            String newBearing = mBearingSpinner.getSelectedItem().toString();
                            displayedImageBearings.get(tRow - 1)[tCol - 1] = newBearing;

                            // extract edited obstacle's id to log
                            int obstacleid = -1;

                            // look for matching entry in obstacleCoord stores coordinates of all the obstacles
                            // update existing obstacleCoord entry that matches the original (x,y) coords with new (x',y') coords
                            for (int m = 0; m < obstacleCoord.size(); m++) {
                                if (Arrays.equals(obstacleCoord.get(m), new int[]{tCol - 1, tRow - 1})) {
                                    obstacleid = m;
                                }
                            }
                            if (((tCol - 1)) >= 0 && ((tRow - 1)) >= 0) {
                                showLog("obstacle num, horizontally(cm), vertically(cm), set to new direction at:");
                                Home.printMessage("OBSTACLE" + "," + (obstacleid + 1) + "," + (tCol - 1) * 10 + "," + (tRow - 1) * 10 + "," + newBearing.toUpperCase());
                            } else {
                                showLog("out of grid");
                            }
                            callInvalidate();
                        }
                    });

                    // dismiss
                    mBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

                    mBuilder.setView(mView);
                    AlertDialog dialog = mBuilder.create();
                    dialog.show();
                    Window window = dialog.getWindow();
                    WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                    layoutParams.width = 150;
                    window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                showLog("Exit change obstacle");
            }

            // change robot size and make sure its within the grid
            if (startCoordStatus) {
                // canDrawRobot is True if robot alr on grid
                if (canDrawRobot) {
                    // removes old robot (type robot -> type unexplored) when user changes robot startpoint
                    for (int col = 0; col < 21; col++) {
                        for (int row = 0; row < 21; row++) {
                            if (cells[col][row].type.equals("robot") || cells[col][row].type.equals("robotfront")) {
                                cells[col][row].setType("unexplored");
                            }
                        }
                    }

                // canDrawRobot is False if robot not yet on grid
                } else
                    canDrawRobot = true;

                // "Set Start Point" button clicked, sets startCoordStatus = true
                // only when startCoordStatus = true can we edit curcoord
                setStartCoord(colNum, rowNum);
                // reset startCoordStatus = false
                startCoordStatus = false;

                // default robot direction to up if None
                String direction = getRobotDirection();
                if (direction.equals("None")) {
                    direction = "up";
                }
                try {
                    int directionInt = 0;
                    switch (direction) {
                        case "up":
                            directionInt = 0;
                            break;
                        case "right":
                            directionInt = 1;
                            break;
                        case "down":
                            directionInt = 2;
                            break;
                        case "left":
                            directionInt = 3;
                            break;
                    }
                    showLog("starting " + "(" + (rowNum - 1) + ","
                            + (colNum - 1) + "," + directionInt + ")");
                } catch (Exception e) {
                    e.printStackTrace();
                }

//                updateRobotAxis(colNum, rowNum, direction);
                if (setStartPointToggleBtn.isChecked()) {
                    setStartPointToggleBtn.toggle();
                    setStartPointToggleBtn.setBackgroundResource(R.drawable.border_black);
                }
                // update visual display
                this.invalidate();
                return true;
            }

            // place obstacles on map
            if (setObstacleStatus) {
                if ((1 <= rowNum && rowNum <= 20) && (1 <= colNum && colNum <= 20)) { // if touch is within the grid
                    int coordX = colNum-1;
                    int coordY = rowNum-1;
                    // for cells
                    int row = convertRow(rowNum);
                    int col = colNum;

//                    // log robot location to see if clash
//                    for (int i = 0; i < 21; i++) {
//                        for (int j = 0; j < 21; j++) {
//                            if (cells[i][j].type.equals("robot") || cells[i][j].type.equals("robotfront")) {
//                                showLog("Gyatt" + i + "," + j);
//                            }
//                        }
//                    }
                    if (!displayedImageIDs.get(rowNum - 1)[colNum - 1].equals("")
                            || !displayedImageBearings.get(rowNum - 1)[colNum - 1].equals("")) {
                        showLog("An obstacle is already at drop location");
                        showToast("Invalid Location: Clash with Other Obstacle");
                    }
                    else if ("robotfront".equals(cells[col][row].type) || "robot".equals(cells[col][row].type)) {
//                        showLog("gyat" + rowNum + " "+ colNum);
                        showLog("Obstacle clashes with robot at drop location");
                        showToast("Invalid Location: Clash with Robot");
                    }
                    else {
                        // get user input from spinners in MapTabFragment static values
                        // imageID is "", imageBearing is "North"
                        String imageID = (MappingFragment.imageID).equals("Nil") ?
                                "" : MappingFragment.imageID;
                        String imageBearing = MappingFragment.imageBearing;

                        // after init, at stated col and row, add the id to use as ref to update grid
                        displayedImageIDs.get(rowNum - 1)[colNum - 1] = imageID;
                        displayedImageBearings.get(rowNum - 1)[colNum - 1] = imageBearing;


                        // this function affects obstacle turning too
                        this.setObstacleCoord(colNum, rowNum);
                    }
                }
                this.invalidate();
                return true;
            }
            if (setExploredStatus) {
                cells[colNum][20 - rowNum].setType("explored");
                this.invalidate();
                return true;
            }

            // added removing imageID and imageBearing
            if (unSetCellStatus) {
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                cells[colNum][20 - rowNum].setType("unexplored");
                for (int i = 0; i < obstacleCoord.size(); i++)
                    if (obstacleCoord.get(i)[0] == colNum && obstacleCoord.get(i)[1] == rowNum)
                        obstacleCoord.remove(i);
                displayedImageIDs.get(rowNum)[colNum - 1] = "";  // remove imageID
                displayedImageBearings.get(rowNum)[colNum - 1] = "";  // remove bearing
                this.invalidate();
                return true;
            }
        }
        showLog("Exiting onTouchEvent");
        return false;
    }

    public void deselectOtherButtons(String buttonName) {
        // buttonName is the current button being clicked
        // this function unchecks/resets check state of all buttons other than buttonName

        // setStartPointToggleBtn is the "set start point" button
        ToggleButton setStartPointToggleBtn = ((Activity) this.getContext())
                .findViewById(R.id.startpointToggleBtn);
        // obstacleImageBtn is the "tree"/"add obstacle" button
        ImageButton obstacleImageBtn = ((Activity) this.getContext())
                .findViewById(R.id.addObstacleBtn);

        // If the button clicked is not "setStartPointToggleBtn" but setStartPointToggleBtn is checked, uncheck it and reset its background.
        if (!buttonName.equals("setStartPointToggleBtn"))
            if (setStartPointToggleBtn.isChecked()) {
                this.setStartCoordStatus(false);
                setStartPointToggleBtn.toggle();
                setStartPointToggleBtn.setBackgroundResource(R.drawable.border_black);
            }
        // If the button clicked is not "obstacleImageBtn" but obstacleImageBtn is checked, uncheck it and reset its background.
        if (!buttonName.equals("obstacleImageBtn"))
            if (obstacleImageBtn.isEnabled()) {
                this.setSetObstacleStatus(false);
                obstacleImageBtn.setBackgroundResource(R.drawable.border_black);
            }
    }


    public void resetRobotCoordinate() {
        showLog("Entering resetRobotCoordinate");
        TextView robotStatusTextView = ((Activity) this.getContext())
                .findViewById(R.id.robotStatus);

        updateRobotAxis(1, 1, "None");

        robotStatusTextView.setText("Not Available");

        startCoord = new int[]{-1, -1};
        curCoord = new int[]{-1, -1};
        oldCoord = new int[]{-1, -1};
        robotDirection = "None";
        canDrawRobot = false;
        validPosition = false;

        showLog("Exiting resetRobotCoordinate");
        this.invalidate();
    }
    public void resetMap() {
        showLog("Entering resetMap");
        TextView robotStatusTextView = ((Activity) this.getContext())
                .findViewById(R.id.robotStatus);

        updateRobotAxis(1, 1, "None");

        robotStatusTextView.setText("Not Available");


        this.deselectOtherButtons("None");

        startCoord = new int[]{-1, -1};
        curCoord = new int[]{-1, -1};
        oldCoord = new int[]{-1, -1};
        robotDirection = "None";
        obstacleCoord = new ArrayList<>();
        mapDrawn = false;
        canDrawRobot = false;
        validPosition = false;

        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                displayedImageIDs.get(i)[j] = "";
                displayedImageBearings.get(i)[j] = "";
            }
        }
        showLog("Exiting resetMap");
        this.invalidate();
    }

    // e.g obstacle is on right side of 3x3 and can turn left and vice versa
    public void moveRobot(String btnDirection) {
        showLog("Entering moveRobot");
        setValidPosition(false);
        int[] curCoord = this.getCurCoord();
        this.setOldRobotCoord(curCoord[0], curCoord[1]);
        int[] oldCoord = this.getOldRobotCoord();
        // list of all the obstacles
        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;

        Integer[] newCoords = Arrays.stream(curCoord).boxed().toArray( Integer[]::new );

        Map.Entry<String, ArrayList<Integer[]>> entry;

        switch (btnDirection) {
            case "forward":
            case "back": {
                Integer[] nextCoord = Straight.straight(newCoords, robotDirection, btnDirection);
                int nextXCoord = nextCoord[0];
                int nextYCoord = nextCoord[1];
                // if out of range, dont update new coordinate
                if (!((nextXCoord >= 1 && nextXCoord <= 18) && (nextYCoord >= 1 && nextYCoord <= 18))) {
                    break;
                }
                // if still in range, update new coordinate
                showLog("Enter checking for obstacles in destination 3x3 grid");
                setCurCoord(nextXCoord+1, nextYCoord+1, robotDirection);
//                int colNum = curCoord[0] + 1;
//                int rowNum = curCoord[1] + 1;

                showLog("New coor is at: " + curCoord[0] + "," + curCoord[1]);
                // update the explored cells
//                cells[colNum+1][20 - rowNum - 2].setType("explored");
//                cells[colNum+2][20 - rowNum - 2].setType("explored");
//                cells[colNum+3][20 - rowNum - 2].setType("explored");
            }
                break;

            case "left": {
                entry = Turn.turn(newCoords,robotDirection,"left");
                // as turn gives a list of points along turning path, extract last point
                Integer[] nextCoord = entry.getValue().get(entry.getValue().size() - 1);
                robotDirection = entry.getKey();
                int nextXCoord = nextCoord[0];
                int nextYCoord = nextCoord[1];
                // if out of range, dont update new coordinate
                if (!((nextXCoord >= 1 && nextXCoord <= 18) && (nextYCoord >= 1 && nextYCoord <= 18))) {
                    break;
                }
                // if still in range, update new coordinate
                showLog("Enter checking for obstacles in destination 3x3 grid");
                setCurCoord(nextXCoord+1, nextYCoord+1, robotDirection);
                showLog("New coor is at: " + curCoord[0] + "," + curCoord[1]);
            }
                break;

            case "right": {
                entry = Turn.turn(newCoords,robotDirection,"right");
                Integer[] nextCoord = entry.getValue().get(entry.getValue().size() - 1);                robotDirection = entry.getKey();
                robotDirection = entry.getKey();
                int nextXCoord = nextCoord[0];
                int nextYCoord = nextCoord[1];
                // if out of range, dont update new coordinate
                if (!((nextXCoord >= 1 && nextXCoord <= 18) && (nextYCoord >= 1 && nextYCoord <= 18))) {
                    break;
                }
                // if still in range, update new coordinate
                showLog("Enter checking for obstacles in destination 3x3 grid");
                setCurCoord(nextXCoord+1, nextYCoord+1, robotDirection);
                showLog("New coor is at: " + curCoord[0] + "," + curCoord[1]);
            }
                break;

            // testing new direction of movement (facing forward)
            case "backleft": {
                entry = Turn.turn(newCoords,robotDirection,"backleft");
                Integer[] nextCoord = entry.getValue().get(entry.getValue().size() - 1);
                robotDirection = entry.getKey();
                int nextXCoord = nextCoord[0];
                int nextYCoord = nextCoord[1];
                // if out of range, dont update new coordinate
                if (!((nextXCoord >= 1 && nextXCoord <= 18) && (nextYCoord >= 1 && nextYCoord <= 18))) {
                    break;
                }
                // if still in range, update new coordinate
                showLog("Enter checking for obstacles in destination 3x3 grid");
                setCurCoord(nextXCoord+1, nextYCoord+1, robotDirection);
                showLog("New coor is at: " + curCoord[0] + "," + curCoord[1]);
            }
                break;

            case "backright": {
                entry = Turn.turn(newCoords,robotDirection,"backright");
                Integer[] nextCoord = entry.getValue().get(entry.getValue().size() - 1);
                robotDirection = entry.getKey();
                int nextXCoord = nextCoord[0];
                int nextYCoord = nextCoord[1];
                // if out of range, dont update new coordinate
                if (!((nextXCoord >= 1 && nextXCoord <= 18) && (nextYCoord >= 1 && nextYCoord <= 18))) {
                    break;
                }
                // if still in range, update new coordinate
                showLog("Enter checking for obstacles in destination 3x3 grid");
                setCurCoord(nextXCoord+1, nextYCoord+1, robotDirection);
                showLog("New coor is at: " + curCoord[0] + "," + curCoord[1]);
            }
                break;
            default:
                robotDirection = "error up";
                break;
        }
//

//        showLog("Enter checking for obstacles in destination 2x2 grid");
//        if (getValidPosition())
//            // check obstacle for new position
//            for (int x = curCoord[0] - 1; x <= curCoord[0]; x++) {
//                for (int y = curCoord[1] - 1; y <= curCoord[1]; y++) {
//                    for (int i = 0; i < obstacleCoord.size(); i++) {
//                        showLog("x-1 = " + (x - 1) + ", y = " + y);
//                        showLog("obstacleCoord.get(" + i + ")[0] = " + obstacleCoord.get(i)[0]
//                                + ", obstacleCoord.get(" + i + ")[1] = " + obstacleCoord.get(i)[1]);
//                        if (obstacleCoord.get(i)[0] == (x - 1) && obstacleCoord.get(i)[1] == y) { // HERE x
//                            setValidPosition(false);
//                            robotDirection = backupDirection;
//                            break;
//                        }
//                    }
//                    if (!getValidPosition())
//                        break;
//                }
//                if (!getValidPosition())
//                    break;
//            }
//        showLog("Exit checking for obstacles in destination 2x2 grid");
        int colNum = curCoord[0] + 1;
        int rowNum = curCoord[1] + 1;
        int oldColNum = oldCoord[0] + 1;
        int oldRowNum = oldCoord[1] + 1;
//        if (getValidPosition())
//            // recall setcurcoord takes in rowNum, colNum
//            this.setCurCoord(colNum, rowNum, robotDirection);
//        else {
//            if (btnDirection.equals("forward") || btnDirection.equals("back"))
//                robotDirection = backupDirection;
//            this.setCurCoord(oldColNum, oldRowNum, robotDirection);
//        }
        this.invalidate();
        showLog("Exiting moveRobot");
    }

    private static class MyDragShadowBuilder extends View.DragShadowBuilder {
        private Point mScaleFactor;

        // Defines the constructor for myDragShadowBuilder
        public MyDragShadowBuilder(View v) {
            // Stores the View parameter passed to myDragShadowBuilder.
            super(v);
        }

        // Defines a callback that sends the drag shadow dimensions and touch point back to the system.
        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {
            // Defines local variables
            int width;
            int height;

            // Sets the width of the shadow to half the width of the original View
            width = (int) (cells[1][1].endX - cells[1][1].startX);

            // Sets the height of the shadow to half the height of the original View
            height = (int) (cells[1][1].endY - cells[1][1].startY);

            // Sets the size parameter's width and height values. These get back to the system through the size parameter.
            size.set(width, height);
            // Sets size parameter to member that will be used for scaling shadow image.
            mScaleFactor = size;

            // Sets the touch point's position to be in the middle of the drag shadow
            touch.set(width / 2, height / 2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            // Draws the ColorDrawable in the Canvas passed in from the system.
            canvas.scale(mScaleFactor.x/(float)getView().getWidth(),
                    mScaleFactor.y/(float)getView().getHeight());
            getView().draw(canvas);
        }

    }

    // week 8 req to update robot pos when alg sends updates
    public void performAlgoCommand(int x, int y, String direction) {
        showLog("Enter performAlgoCommand");
        showLog("x = " + x + "\n" + "y = " + y);
        if ((x > 1 && x < 21) && (y > -1 && y < 20)) {
            showLog("within grid");
            robotDirection = (robotDirection.equals("None")) ? "up" : robotDirection;
            switch (direction) {
                case "N":
                    robotDirection = "up";
                    break;
                case "S":
                    robotDirection = "down";
                    break;
                case "E":
                    robotDirection = "right";
                    break;
                case "W":
                    robotDirection = "left";
                    break;
            }
        }
        // if robot pos was not set initially, don't set as explored before moving to new coord
        if (!(curCoord[0] == -1 && curCoord[1] == -1)) {
            showLog("if robot was not at invalid pos prev");
            if ((curCoord[0] > 1 && curCoord[0] < 21) && (curCoord[1] > -1 && curCoord[1] < 20)) {
                showLog("prev pos was within grid");
                for (int i = curCoord[0] - 1; i <= curCoord[0]; i++) {
                    for (int j = curCoord[1] - 1; j <= curCoord[1]; j++) {
                        if (!(cells[i][20-j-1]).type.equals("obstacle")){
                            cells[i][20 - j - 1].setType("explored");
                        }
                    }
                }
            }
        }
        // if robot is still in frame
        if ((x > 1 && x < 21) && (y > -1 && y < 20)) {
            showLog("within grid");
            setCurCoord(x, y, robotDirection);    // set new coords and direction
            canDrawRobot = true;
        }
        // if robot goes out of frame
        else {
            showLog("set canDrawRobot to false");
            canDrawRobot = false;
            curCoord[0] = -1;
            curCoord[1] = -1;
        }
        this.invalidate();
        showLog("Exit performAlgoCommand");
    }

    // Modified for algo side to denote each obstacle as: (algoX, algoY, algoDirection, algoObsId)
    // 2nd half of getObstacles()
    public String translateCoord(String msg){
        String translatedMsg = "";
        // split msg by '|'
        String[] msgSections = msg.split("\\|");
        for(int i = 1; i < msgSections.length; i++) {   // ignore 1st sub string since its "ALG"
            String[] msgSubSections = msgSections[i].split(",");
            // algoX and algoY are 'related' to (x, y) coordinates on a 0-indexed grid, e.g. (10, 7) in (x, y) = (105, 75) in (algoX, algoY)
            int algoX = Integer.parseInt(msgSubSections[0]) * 10 + 5;
            int algoY = Integer.parseInt(msgSubSections[1]) * 10 + 5;
            // algoDirection is a mapping of 4 values for each direction: North = 90, East = 0, South = -90, West = 180
            int algoDirection;
            switch(msgSubSections[2].charAt(0)) {
                case 'N':
                    algoDirection = 90;
                    break;
                case 'S':
                    algoDirection = -90;
                    break;
                case 'E':
                    algoDirection = 0;
                    break;
                case 'W':
                    algoDirection = 180;
                    break;
                default:    // should not happen (in theory)
                    showLog("Invalid direction character!");
                    algoDirection = -1;
            }
            // algo_obs_id is zero-index obstacle id number, probably can just use a for loop w/ i < obstacleCoord.size()? Assuming that it doesn't affect the algo
            int algoObsId = Integer.parseInt(msgSubSections[3]);
            translatedMsg += algoX + "," + algoY + "," + algoDirection + "," + algoObsId;
//            obstList.add(new int[]{algoX, algoY, algoDirection, algoObsId});
            if(i < msgSections.length - 1) translatedMsg += "|";  // add separator for all obstacles except the last
        }
        // The '_' is just a special character to denote the position to split this resulting string later on
        return msg + "_ALG|" + translatedMsg;
    }

    public static String saveObstacleList(){    // used for the save/load map functionality
        String message ="";
        for (int i = 0; i < obstacleCoord.size(); i++) {
            message += ((obstacleCoord.get(i)[0]) + "," // add x coordinate of obstacle
                    + (obstacleCoord.get(i)[1]) + ","   // add y coordinate of obstacle
                    + displayedImageBearings.get(obstacleCoord.get(i)[1])[obstacleCoord.get(i)[0]].charAt(0));  // add the 1st letter of the direction

//            showLog("here"+imageBearings.get(obstacleCoord.get(i)[1])[obstacleCoord.get(i)[0]]);

            if(i < obstacleCoord.size() - 1) message += "\n";    // add a "|" to the end of each obstacle's info (except for the last)
        }
        BluetoothCommunications.getMessageReceivedTextView().append(message+"\n");

        return message;

    }

    // Returns a string that contains 2 'substrings' of the obstacle information, 1 untranslated & 1 translated, separated by '_'
    // Currently it will send all the coordinates regardless of whether the 'obstacle' is an obstacle or image - but images have '-1' as obstacle id
    public String getObstacles() {
        String msg = "ALG|";
        int obstId = 0;
        for (int i = 0; i < obstacleCoord.size(); i++) {
            // check if its an obstacle or an image
            int col = obstacleCoord.get(i)[0];
            int row = obstacleCoord.get(i)[1];
            // Additional redundant logic to allow for position of obstacles w/o images to also be sent over
            msg += (col + ","     // x
                    + row + ","   // y
                    + displayedImageBearings.get(obstacleCoord.get(i)[1])[obstacleCoord.get(i)[0]].charAt(0) + ",");   // direction
            if(displayedImageIDs.get(row)[col] == null || displayedImageIDs.get(row)[col].equals("") || displayedImageIDs.get(row)[col].equals("Nil")) { // ITEM_LIST value is null, but in obstacleCoord => obstacle
                msg += obstId++;   // obstacle id
            } else { // ITEM_LIST not empty, but in obstacleCoord => image (or blank obstacle)
                msg += (-1);    // non-obstacle id
            }
            if (i < obstacleCoord.size() - 1) msg += "|";
        }

        // Add the translated message to msg
        msg = translateCoord(msg);

        return msg;
    }

    // Updating the obstacle image id (sent over by RPi)
    public boolean updateIDFromRpi(String obstacleID, String imageID) {
        showLog("updateIDFromRpi");
        int x = obstacleCoord.get(Integer.parseInt(obstacleID))[0];
        int y = obstacleCoord.get(Integer.parseInt(obstacleID))[1];
        displayedImageIDs.get(y)[x] = (imageID.equals("-1")) ? "NA" : imageID;
        this.invalidate();
        return true;
    }
    private void updateStatus(String message) {
        Toast toast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP,0, 0);
        toast.show();
    }

}