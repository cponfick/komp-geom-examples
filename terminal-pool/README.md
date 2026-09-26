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
- Click or press `Space`/`Enter` to shoot
- Press `A`/Left Arrow and `D`/Right Arrow for fine aiming
- Press `R` to reset
- Press `Escape` to quit

## Rules implemented

- Two-player turns with automatic switching
- Groups are assigned after the first legal pocket (solids or stripes)
- Players must hit their own group first
- Pocketing the cue ball is a foul and gives the other player ball in hand
- The eight ball can only be pocketed after the player's group is cleared
- Pocketing the eight ball early loses the game
- A legal pocketed shot lets the current player continue
