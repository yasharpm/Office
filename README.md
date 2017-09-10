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
	compile 'com.yashoid:office:1.0.1'
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
- `cancel` can be called to cancel the execution of a task. It will return false if the task is in execution or is already finished or not found.

Some libraries like Picasso or Retrofit can be fed an `Executor` to perform their async tasks on. Call `TaskManager.getExecutor()` to get an `Executor` instance on your desired section and your preferred priority.

### Async Operation
