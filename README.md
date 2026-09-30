# Disembodiment

Disembodiment is a Minecraft mod that adds a survival-balanced way to temporarily explore the world in a spectator-like mode.

The mod adds a custom block, the **Dematerializer**, which consumes a custom fuel item called **Inkor**. When activated, the Dematerializer temporarily places the player into a spectator-like ghost state, allowing them to freely move through walls and explore otherwise inaccessible areas. When the effect expires, the player is returned to the position where they originally activated the block.

The goal was to make spectator-style exploration usable as an intentional survival mechanic.

## Features

- **Dematerializer** — a custom block that temporarily dematerializes the player
- **Inkor** — a consumable fuel item used by the Dematerializer
- Temporary spectator-style noclip
- Automatic return to the player's original position when the effect ends
- Multiple Inkors can be loaded to increase the duration of the effect
- Small custom structures generated throughout the Overworld
- Client-side configuration for visual and sound effects
- Server-side configuration for gameplay values
- Multiplayer support with the mod installed on both client and server

By default, each Inkor provides **15 seconds** of dematerialization time.

## Technical Overview

The main technical focus of the project was implementing the Dematerializer as a custom block entity with its own persistent state and behavior.

The block tracks its current state and stored fuel while handling the player's transition into and out of the dematerialized state. The system also preserves information about the player before activation so that their original position and gameplay state can be properly restored when the effect ends.

To keep this behavior organized, the Dematerializer uses a state-management system rather than placing all of its logic directly inside the block entity.

Working on the project involved:

- Creating and registering custom blocks, items, and block entities
- Designing state-based behavior for the Dematerializer
- Managing transitions between inactive, active, and completed behavior
- Persisting block entity data between game updates and world saves
- Tracking player state and position across the dematerialization period
- Temporarily changing and restoring player game state
- Consuming and managing custom fuel
- Adding configurable gameplay values
- Implementing client-side audiovisual effects
- Adding custom world-generation structures
- Debugging behavior within Minecraft's event-driven game lifecycle

## Building

The project includes a Gradle wrapper, so a separate Gradle installation is not required.

Clone the repository:

```bash
git clone https://github.com/charlieomiller/disembodiment.git
cd disembodiment
```

Build on Windows:

```bash
gradlew.bat build
```

Build on Linux or macOS:

```bash
./gradlew build
```

The compiled mod will be generated in:

```text
build/libs/
```

## What I Learned

Disembodiment gave me experience building functionality inside a much larger existing system rather than controlling the entire application myself.

Implementing the Dematerializer required understanding Minecraft's block entity lifecycle, player state, persistence, world updates, configuration systems, and client/server behavior. The project also gave me practical experience organizing behavior around explicit states instead of allowing increasingly complex conditional logic to accumulate inside a single class.

## What I Would Do Differently

Move active dematerialization state out of the Dematerializer block entity and into player or server-scoped state. The current design depends on the block entity the player interacted with remaining loaded, which can cause problems if the player travels far enough to unload the chunk it is in. Separating the active session from the block would make the mechanic much more reliable.
