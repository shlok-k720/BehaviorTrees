# Behavior Trees

A Java and Kotlin behavior-tree library for FTC robots. The library separates reusable tree logic from FTC-specific integration so that decision-making code can be tested on the JVM before it runs on a robot.

## Modules

| Artifact | Purpose |
| --- | --- |
| `core` | Platform-independent behavior-tree nodes, builders, blackboard, and logging. This is the dependency most robot projects need. |
| `ftc` | Android/FTC-facing module that depends on `core` and compiles against the FTC SDK. |

## Install from JitPack

This project is configured to publish each module as a JitPack artifact. Add JitPack to the consuming project's repositories:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then depend on the module required by the robot project:

```kotlin
dependencies {
    implementation("com.github.shlok-k720.BehaviorTrees:core:<version>")
    // implementation("com.github.shlok-k720.BehaviorTrees:ftc:<version>")
}
```

Replace `<version>` with a release tag, such as `v0.1.0`, after it has been built by JitPack. The `ftc` artifact already brings in `core`.

## Building a Tree

Every `BehaviorNode` returns one of three statuses:

| Status | Meaning |
| --- | --- |
| `RUNNING` | Work is still in progress; tick the tree again on the next loop. |
| `SUCCESS` | The node completed its goal. |
| `FAILURE` | The node could not complete its goal or was invalidated. |

Call `tree.tick()` once per OpMode loop. A node runs its start callback once for an activation, its tick callback on every loop while active, and its end callback when it finishes or is invalidated.

### Kotlin DSL

```kotlin
import com.rmsftc.bttree.NodeStatus
import com.rmsftc.bttree.blackboard.Blackboard
import com.rmsftc.bttree.builders.KotlinBuilder

val blackboard = Blackboard()

val tree = KotlinBuilder.rootTree(blackboard) {
    sequence("score preload") {
        condition("has target") { blackboard.has("target") }
        action(
            name = "drive to target",
            start = { drivetrain.beginDrive() },
            tick = {
                if (drivetrain.atTarget()) NodeStatus.SUCCESS else NodeStatus.RUNNING
            },
            end = { drivetrain.stop() }
        )
    }
}

// In the OpMode loop:
tree.tick()
```

### Java Builder

```java
Blackboard blackboard = new Blackboard();

ReactiveSelectorNode tree = JavaBuilder.rootTree(blackboard, root ->
    root.sequence("score preload", sequence -> {
        sequence.condition("has target", () -> blackboard.has("target"));
        sequence.action(
            "drive to target",
            () -> drivetrain.beginDrive(),
            () -> drivetrain.atTarget() ? NodeStatus.SUCCESS : NodeStatus.RUNNING,
            () -> drivetrain.stop()
        );
    })
);

// In the OpMode loop:
tree.tick();
```

## Node Types

- **`ConditionNode`** evaluates a boolean predicate and returns `SUCCESS` or `FAILURE`.
- **`ActionNode`** adapts start, tick, and end callbacks into a behavior node.
- **`SequenceNode`** processes children in order; it fails on the first failure and succeeds only when all children succeed.
- **`ReactiveSelectorNode`** re-evaluates children from highest priority on every tick. A higher-priority child can preempt a lower-priority child that is running.
- **`ParallelNode`** ticks all unfinished children. Use `ParallelPolicy.JOIN` to wait for all children, `SUCCESS` to require all children to succeed, or `FAILURE` to fail only when all children fail.
- **`TimeoutNode`** ticks its child until the duration in milliseconds elapses, then invalidates the child and returns `FAILURE`. `waitMs` in either builder creates this timeout wrapper around a nested reactive selector.

Call `reset()` before reusing a tree when its state should return to never-ticked. Finished nodes also start fresh when ticked again, while `invalidate()` aborts only a currently running node.

## Blackboard, Callbacks, and Logging

`Blackboard` is typed shared state for tree nodes. It accepts both string and enum keys:

```kotlin
blackboard.set("target", "backdrop")
val target = blackboard.get("target", String::class.java)
```

Register callbacks for changes that must take effect even if the corresponding branch is not currently being ticked:

```kotlin
blackboard.registerCallback("emergency_stop") { _, value ->
    if (value == true) drivetrain.stop()
}
```

Callbacks remain registered until they are explicitly deregistered, their key is removed, or `clearAll()` is called. Build a tree once per blackboard, or clear/deregister its callbacks before rebuilding it.

Attach a `LogSystem` to receive node lifecycle events:

```kotlin
Logger.attachLogger { caption, data, timestampMs ->
    telemetry.addData(caption, "$timestampMs $data")
}
```

## Tests

Run the core test suite:

```bash
./gradlew :core:test
```

Run the aggregate suite explicitly:

```bash
./gradlew :core:test --tests com.rmsftc.bttree.UnitTestAll
```

## Publishing

The publishing setup follows the [FTC library publishing guide](https://shlok-k720.github.io/documentation-guides/guides/ftc_library_publishing_guide.html#jitpack-build-files):

1. Update `VERSION_NAME` in `gradle.properties` to match the release tag without its leading `v`.
2. Verify the publishable modules locally:

   ```bash
   ./gradlew clean :core:build :ftc:assembleRelease
   ```

3. Commit the version change, create and push the matching tag, for example `v0.1.0`.
4. Open the repository on JitPack and wait for the tagged build to succeed before using the version in a consumer project.

JitPack builds this repository with the OpenJDK 17 configuration in `jitpack.yml`. The published `core` artifact targets JVM 8, and the `ftc` artifact is an Android library that marks FTC SDK dependencies as `compileOnly` so consuming robot projects provide their own SDK runtime.
