# Office
Office is a powerful threading tool that can have multiple thread groups in the back and allows for defining dependencies for running tasks.

Office is meant to bring maximum flexibily on managing threads while making it as simple and straight forward as possible.

<img src="https://user-images.githubusercontent.com/4597931/30242834-26586b70-95b3-11e7-9613-1c6f85684bf4.gif" width="400px"/>

Complicated async operations are consisted of multiple tasks. Imagine an operation in which you have to sync some imagines from the server.
- A First you will have to make a network request to get the image list.
- B Second to store the received data in some database.
- C Third to download each image.
- D And forth for each received image do some process on them.

You don't want to make other async operations to wait for this process to end. Meanwhile adding too many threads will only slow things down rather than improving them. Office reduces solving this problem to just a few lines of code.


## Usage

Add the dependency:
```Groovy
dependencies {
	compile 'com.yashoid:office:1.2.0'
}
```

## How to use this library

### Jump in
Here is the code to solve the problem above.

```java
TaskManager taskManager = new TaskManagerBuilder()
    .addNetworkSection(5)
    .addDatabaseReadWriteSection(1)
    .addCalculationSection(4)
    .build();
 
TaskGroup A = OperationBuilder.singleTask(TaskManager.NETWORK, getImageListFromNetworkRunnable);
TaskGroup B = OperationBuilder.singleTask(TaskManager.DATABASE_READWRITE, writeImageListInDatabaseRunnable);
 
DependencyOperation operation = new OperationBuilder()
    .perform(A).immediately()
    .perform(B).after(A)
    .build();

operation.start();


// Then inside task B code after it finishes the following code will be run for each image.

TaskGroup CD = OperationBuilder.sequentialTasks(
    OperationBuilder.newTasks(TaskManager.NETWORK, downloadImageRunnable),
    OperationBuilder.newTasks(TaskManager.CALCULATION, processImageRunnable)
    );
    
operation.perform(CD).after(B);
```

You idealy want like 5 threads for your network operations, 1 thread to handle the database read and write and 4 threads to do image processing. This definition will be done into a `TaskManager` object.

Then we use a `DepedencyOperation` to manage each task. `DependencyOperation` works with `TaskGroup` objects to define the dependencies. `TaskGroup` holds a number of tasks and can be told to run its tasks sequentially or in parallel. A parallel task group is considered done when all of its tasks have finished running.

### Task Manager
Task manager is the heart of Office. You usually want to keep a singleton instance of your `TaskManager` in your code. Thread groups are defined into `TaskManager` and called a "section". Each one defined with a name to refer to and the number of worker threads inside of it.

A `TaskManager` instance can be created either directly or by using a `TaskManagerBuilder`. There is also a default configuration that can be accessed as a singleton through `DefaultTaskManager.getInstance()`. You can also add some sections later by calling each of `TaskManager.addSection()` methods. Some section names are predefined in `TaskManager` to help use it in different libraries and avoid creating new sections. `TaskManager` instance must be created on the main thread. After creation every other method is thread safe.

Many methods are defined in `TaskManager` to send tasks to it for execution. All of them revolving around the `Runnable` to run, the section to run the `Runnable` in, and its priority. Bigger priority value means execution before other tasks with lower priority. Default priority is zero.

- Methods ending in "AndWait" block the calling thread until the execution finishes.
- Methods ending in "Immediately" will create a disposable new thread to perform the task immediately.
- `TaskManager.MAIN` is the section name reserved for main thread.
- `TaskManager.IMMEDIATELY` is the section name reserved for executing the task on a non-main thread immediately.
- Calling waiting methods on sections that have only one thread (like main section) will cause a dead lock.
- `cancel` can be called to cancel the execution of a task. It will return false if the task is in execution or is already finished or not found.

Some libraries like Picasso or Retrofit can be fed an `Executor` to perform their async tasks on. Call `TaskManager.getExecutor()` to get an `Executor` instance on your desired section and your preferred priority.

### Async Operation
`AsyncOperation` is a similar equivalent to Android's native `AsyncTask`. The difference is you can choose which section the background method gets called on. All `AsyncTask` functionalities have been defined into `AsyncOperation`.

It is also possible to create the `AsyncOperation` outside the main thread. It already has access to it through the `TaskManager`.

### Dependency Operation
`DependencyOperation` is a strong tool in cases that your operation is more complicated than usual. It has the ability to induce dependency on task groups. For any set of `TaskGroup`s you can define a rule to be run after another set of `TaskGroup`s have finished running. A `TaskGroup` itself can be consisted of one or more tasks to be executed in parallal or in a serie.

In order to create a `DependencyOperation` instance you can use the `OperationBuilder`. `OperationBuilder` class has convenient methods to define `TaskGroup`s and then define the dependencies between them. By calling `OperationBuilder.build()` you get the `DependencyOperation` instance. Build method can be called many times to create more operations with the same config. The operation won't start running unless you specifically call `DependencyOperation.start()`.

It is also possible to directly create a `DependencyOperation` through its public constructor. Calling `DependencyOperation.perform()` method will cause the operation to start and immediately react to the `TaskGroup`s you have provided.

- Performed `TaskGroup`s are recorded inside the `DependencyOperation`. So it is guaranteed that the class won't forget it has run it.
- You can keep a `DependencyOperation` instance to perform dependent tasks whenever you want.
- After a `TaskGroup` is performed, the memory for the tasks will be released. So there is no limit on the number of `TaskGroup`s you send to the operation to be run.

### Task Group
A `TaskGroup` holds one or more tasks to be executed in parallel or one after another. You can get a `TaskGroup` instance by calling either of the `OperationBuilder.singleTask()` or `OperationBuilder.sequentialTasks()` or `OperationBuilder.parallelTasks()` methods. These methods are about taking a set of `Runnable`s to run and the section to run them in and the priority to run them with.

There is a special constructor for `TaskGroup` in `OperationBuilder` that received an `Object` to be the "runnerObject" and one or more `String`(s) to be the "runnerMethod"(s). The task will berun using reflection by calling `runnerObject.runnerMethod()`. It is useful when you have the methods defined in your object and saves you from creating a `Runnable` for every task.

- Having a task group is actually redundant because `DependencyOperation`'s rules can provide the same functionality. But it allows for more flexibility in writing code. In most cases you will only need to call `OperationBuilder.singleTask()` methods.
- `TaskGroup` creates instances of `TaskDescriptor` inside of it. If you want the tasks inside a `TaskGroup` to be run on various sections you can create `TaskDescriptor`s directly by calling `OperationBuilder.newTasks()` methods.
- Both `TaskGroup` and `TaskDescriptor` classes are public and can be subclassd or created directly too.
- `DependencyOperation` uses `Object.equals()` methods to determine whether it has performed a task or not. For your special customizations you can subclass a `TaskGroup` and override the "equals" methods. Have in mind that `TaskDescriptor`s inside the `TaskGroup` will be set to `null` afther the `TaskGroup` has finished running.

## Notes
Both `AsyncOperation` and `DependencyOperation` are subclassed from `Operation`. Learning `Operation` class itself in unneccessary. It is only worth mentioning that calling `cancelAllTasks()` on `Operation` subclasses will stop everything. Except the tasks that are already being run. Tasks under execution will never be interrupted in Office.

Office is inspired by how things work in an actual office that has multiple sections with different purposes and each section has multiple employees that when requested to perform a task, will perform certain actions. Hence the name is "Office".
