package com.example.mdp_group_19;

public class Straight {

  public static Integer[] straight(Integer[] startCoord, String direction, String movement) {
    switch (direction) {
      case "up":
        switch (movement) {
          case "forward":
            return new Integer[] {startCoord[0], startCoord[1] + 1};
          case "back":
            return new Integer[] {startCoord[0], startCoord[1] - 1};
        }
      case "right":
        switch (movement) {
          case "forward":
            return new Integer[] {startCoord[0] + 1, startCoord[1]};
          case "back":
            return new Integer[] {startCoord[0] - 1, startCoord[1]};
        }
      case "down":
        switch (movement) {
          case "forward":
            return new Integer[] {startCoord[0], startCoord[1] - 1};
          case "back":
            return new Integer[] {startCoord[0], startCoord[1] + 1};
        }
      case "left":
        switch (movement) {
          case "forward":
            return new Integer[] {startCoord[0] - 1, startCoord[1]};
          case "back":
            return new Integer[] {startCoord[0] + 1, startCoord[1]};
        }
    }

    throw new IllegalStateException("This should never happen");
  }

}
