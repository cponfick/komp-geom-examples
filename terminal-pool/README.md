# Komp-Geom Pool

A small Swing pool game demonstrating Komp-Geom's `Vec2` and `Seg2` types. It uses a standard rack of 15 numbered object balls: seven solids, the eight ball, and seven stripes. Aim with the mouse and click to shoot, or use the keyboard controls.

## Run from source

```bash
./gradlew run
```

## Build and run the executable JAR

```bash
./gradlew jar
java -jar build/libs/terminal-pool-0.1.0.jar
```

## Controls

- Move the mouse to aim
- Move the mouse to aim
- Hold the mouse button, then release to charge and shoot
- Hold `Space` or `Enter`, then release to charge and shoot
- The aiming guide grows as shot power increases
- Press `R` to reset
- Press `R` to reset
- Press `Escape` to quit

## Rules

This is a deliberately simple solo demonstration:

- Pocket any object ball in any order.
- The eight ball must be the last object ball pocketed.
- Pocketing the eight ball early ends the game.
- Scratching resets the cue ball without adding a penalty.
- Press `R` to start over.
