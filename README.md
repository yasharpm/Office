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

```Java
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
