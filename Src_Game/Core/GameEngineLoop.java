package core;

/**
 * Vòng lặp trò chơi chuẩn theo Robert Nystrom (Game Programming Patterns - Ch. 9).
 * Sử dụng Fixed Timestep cho logic vật lý (đảm bảo tính nhất quán trên mọi máy)
 * và tách rời chu kỳ vẽ đồ họa với hệ số alpha nội suy.
 */
public abstract class GameEngineLoop implements Runnable {
    private final double timeStepSeconds; // Ví dụ: 1.0 / 60.0 = 0.016667s
    private volatile boolean isRunning = false;
    private Thread loopThread;

    public GameEngineLoop(int targetUpdatesPerSecond) {
        this.timeStepSeconds = 1.0 / targetUpdatesPerSecond;
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;
        loopThread = new Thread(this, "GameEngineLoop-Thread");
        loopThread.start();
    }

    public synchronized void stop() {
        isRunning = false;
        if (loopThread != null) {
            try {
                loopThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        long previousTime = System.nanoTime();
        double accumulator = 0.0;

        while (isRunning) {
            long currentTime = System.nanoTime();
            double elapsedTime = (currentTime - previousTime) / 1_000_000_000.0;
            previousTime = currentTime;

            // Giới hạn max frame time để chống "Spiral of Death"
            if (elapsedTime > 0.25) {
                elapsedTime = 0.25;
            }

            accumulator += elapsedTime;

            // Vòng lặp cập nhật vật lý với bước thời gian cố định (Fixed Update)
            while (accumulator >= timeStepSeconds) {
                fixedUpdate(timeStepSeconds);
                accumulator -= timeStepSeconds;
            }

            // Tính hệ số nội suy alpha cho việc vẽ màn hình
            float alpha = (float) (accumulator / timeStepSeconds);
            render(alpha);

            // Nghỉ nhẹ 1ms để nhường CPU cho các luồng mạng
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Cập nhật logic vật lý tại chu kỳ thời gian cố định.
     */
    protected abstract void fixedUpdate(double deltaTime);

    /**
     * Vẽ đồ họa với hệ số nội suy giữa 2 khung vật lý.
     */
    protected abstract void render(float alpha);
}
