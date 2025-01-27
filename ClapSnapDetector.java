import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ClapSnapDetector {
    private float threshold;
    private int minFrequency;
    private int maxFrequency;
    private boolean isListening;
    private int detectedCount;
    private String clapAudioPath;
    private String snapAudioPath;

    public ClapSnapDetector(float threshold, int minFrequency, int maxFrequency) {
        this.threshold = threshold;
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;
        this.isListening = false;
        this.detectedCount = 0;
        this.clapAudioPath = "clap.wav";
        this.snapAudioPath = "snap.wav";
    }

    private boolean detectClapSnap(byte[] audioData, float sampleRate) {
        // 将字节数组转换为short数组
        short[] audioShorts = new short[audioData.length / 2];
        for (int i = 0; i < audioShorts.length; i++) {
            audioShorts[i] = (short) ((audioData[i * 2 + 1] << 8) | (audioData[i * 2] & 0xff));
        }

        // 执行FFT
        Complex[] fft = FFT.fft(Arrays.stream(audioShorts).mapToDouble(s -> s / 32768.0).toArray());

        // 计算频率和幅度
        double[] magnitudes = new double[fft.length / 2];
        for (int i = 0; i < magnitudes.length; i++) {
            magnitudes[i] = fft[i].abs();
        }

        double maxMagnitude = Arrays.stream(magnitudes).max().orElse(0);
        int peakCount = 0;

        for (int i = 0; i < magnitudes.length; i++) {
            double frequency = i * sampleRate / audioShorts.length;
            if (frequency >= minFrequency && frequency <= maxFrequency && magnitudes[i] > threshold * maxMagnitude) {
                peakCount++;
            }
        }

        return peakCount > 0;
    }

    private void playAudio(String filePath) {
        File audioFile = new File(filePath);
        if (audioFile.exists()) {
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
                // 等待音频播放完成
                Thread.sleep(clip.getMicrosecondLength() / 1000);
                clip.close();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("音频文件 " + filePath + " 不存在");
        }
    }

    public void listen() {
        AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            byte[] buffer = new byte[4096];
            System.out.println("开始监听拍手声或啪啪声...");
            isListening = true;

            while (isListening) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (detectClapSnap(buffer, format.getSampleRate())) {
                    detectedCount++;
                    System.out.println("检测到拍手声或啪啪声！ (总计: " + detectedCount + ")");

                    if (detectedCount % 2 == 0) {
                        System.out.println("播放拍手声音效");
                        playAudio(clapAudioPath);
                    } else {
                        System.out.println("播放打响指声音效");
                        playAudio(snapAudioPath);
                    }
                }
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void startListening() {
        Thread listeningThread = new Thread(this::listen);
        listeningThread.start();
    }

    public void stopListening() {
        isListening = false;
    }

    public static void main(String[] args) {
        ClapSnapDetector detector = new ClapSnapDetector(0.3f, 2000, 4000);
        try {
            detector.startListening();
            Thread.sleep(60000); // 运行60秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            detector.stopListening();
            System.out.println("总共检测到 " + detector.detectedCount + " 次拍手声或啪啪声");
        }
    }
}

// FFT类的实现（简化版）
class Complex {
    public final double re;
    public final double im;

    public Complex(double real, double imag) {
        re = real;
        im = imag;
    }

    public double abs() {
        return Math.hypot(re, im);
    }
}

class FFT {
    public static Complex[] fft(double[] x) {
        int n = x.length;
        if (n == 1) return new Complex[]{new Complex(x[0], 0)};
        if (n % 2 != 0) throw new RuntimeException("n is not a power of 2");
        
        Complex[] even = fft(Arrays.copyOfRange(x, 0, n/2));
        Complex[] odd  = fft(Arrays.copyOfRange(x, n/2, n));
        Complex[] y = new Complex[n];
        for (int k = 0; k < n/2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k] = new Complex(even[k].re + wk.re * odd[k].re - wk.im * odd[k].im,
                               even[k].im + wk.re * odd[k].im + wk.im * odd[k].re);
            y[k + n/2] = new Complex(even[k].re - wk.re * odd[k].re + wk.im * odd[k].im,
                                     even[k].im - wk.re * odd[k].im - wk.im * odd[k].re);
        }
        return y;
    }
}

