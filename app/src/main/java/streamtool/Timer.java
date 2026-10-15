package streamtool;

public class Timer {
    Thread timer;
    boolean isRunning = false;
    Long startTime;
    Long nextUpdate;
    int duration;
    int time;
    boolean reverse;
    ClientHandler client;
    String sourceName = "";
    String endMessage = "";

    void newThread() {
        timer = new Thread() {
            public void run() {
                nextUpdate = startTime;
                while(true) {
                    Long currentTime = System.currentTimeMillis();
                    if (!isRunning) break;
                    if (time > duration) break;
                    if (currentTime > nextUpdate) {
                        int displayTime = time;
                        if (reverse) displayTime = duration - time;
                        int minutes = displayTime / 60;
                        int seconds = displayTime - (minutes * 60);
                        String timeString;
                        if (seconds < 10) timeString = minutes + ":0" + seconds; else timeString = minutes + ":" + seconds;
                        if (minutes < 10) timeString = "0" + timeString;
                        client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + sourceName + "\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + timeString + "\"}}}}");
                        setNextUpdate();
                    }

                    Long sleepTime = (nextUpdate - currentTime);
                    if (sleepTime > 0) try {
                        sleep(sleepTime);
                    } catch (InterruptedException e) {}

                }
                if (!endMessage.equals("none")) client.send("{\"op\": 6, \"d\": {\"requestType\": \"SetInputSettings\", \"requestId\": \"0\", \"requestData\": {\"inputName\": \"" + sourceName + "\", \"overlay\": true, \"inputSettings\": {\"text\":\"" + endMessage + "\"}}}}");
                if (time < duration) {
                    Main.seedDone = true;
                    System.out.println("Timer stopped");
                    System.out.println();
                    System.out.print("> ");
                }
            }      
        };
    }

    public Timer(RuntimeData run) {
        isRunning = false;
        time = 0;
        this.client = run.client;
    }

    void start(int duration, boolean reverse, String sourceName, String endMessage) {
        startTime = System.currentTimeMillis();
        this.sourceName = sourceName;
        this.duration = duration;
        this.reverse = reverse;
        this.endMessage = endMessage;
        time = 0;
        isRunning = true;

        if (timer != null && timer.isAlive()) {
            System.out.println("Timer already active, updated time");
        } else {
            System.out.println("Starting new timer");
            newThread();
            timer.start();
        }
    }

    void add(int amount) {
        time += amount;
        startTime -= amount * 1000;
    }

    private void setNextUpdate() {
        time++;
        nextUpdate = startTime + (1000 * time);
    }

    void stop() {
        System.out.println("Stopping timer");
        isRunning = false;
    }
}
