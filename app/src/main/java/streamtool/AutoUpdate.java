package streamtool;

public class AutoUpdate {

    boolean isRunning;
    RuntimeData run;

    public AutoUpdate(RuntimeData run) {
        this.run = run;
        isRunning = false;
    }

    void start() {
        new Thread() {
            public void run() {
                isRunning = true;
                while(isRunning) {

                    if (!run.spectating) {
                        run.commandManager.execute(run.commandParser.getCommand("update nomsg"));
                    }

                    try {
                        sleep(run.updateInterval);
                    } catch (InterruptedException e) {}

                    if (!run.timer.isRunning) isRunning = false;
                    if (run.timer.time + (Main.abDelay / 1000) > run.timer.duration) isRunning = false;
                }
            }      
        }.start();
    }

    void stop() {
        isRunning = false;
    }
    
}
