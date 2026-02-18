import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateFlowZhPng {
  static class Step {
    String title;
    String d1;
    String d2;
    Step(String title, String d1, String d2) {
      this.title = title;
      this.d1 = d1;
      this.d2 = d2;
    }
  }

  public static void main(String[] args) throws Exception {
    int w = 1000;
    int h = 1700;

    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    Color bg = new Color(250, 249, 246);
    Color band = new Color(226, 236, 246);
    Color card = new Color(242, 242, 242);
    Color stroke = new Color(45, 45, 45);
    Color text = new Color(25, 25, 25);
    Color subtext = new Color(70, 70, 70);

    g.setColor(bg);
    g.fillRect(0, 0, w, h);

    // Title band
    int tx = 50, ty = 30, tw = 900, th = 70;
    g.setColor(band);
    g.fill(new RoundRectangle2D.Double(tx, ty, tw, th, 14, 14));
    g.setColor(stroke);
    g.setStroke(new BasicStroke(2f));
    g.draw(new RoundRectangle2D.Double(tx, ty, tw, th, 14, 14));

    Font titleFont = new Font("SansSerif", Font.BOLD, 34);
    g.setFont(titleFont);
    g.setColor(text);
    g.drawString("Gatling 壓測流程（中文詳細版）", 80, 76);

    Step[] steps = new Step[] {
      new Step("1. 執行命令", "mvnw gatling:test", "指定 Simulation 類別開始壓測流程"),
      new Step("2. Maven 建置", "test-compile + gatling-maven-plugin", "編譯測試程式並啟動模擬"),
      new Step("3. 載入測試資源", "data/users.csv 作為 feeder", "bodies/login.json 可作 POST/PUT request template"),
      new Step("4. HTTP 協定設定", "設定 baseUrl、accept header", "套用 inferHtmlResources 過濾規則"),
      new Step("5. 定義 Scenario", "feed -> GET /delay/1", "check status 200 -> pause 1s"),
      new Step("6. 注入虛擬使用者", "rampUsers(10).during(10s)", "10 秒內逐步拉升到 10 位使用者"),
      new Step("7. 執行並收集指標", "送出請求並回收結果", "統計 latency、throughput、OK/KO"),
      new Step("8. 記錄請求明細", "依 logback.xml 設定輸出", "target/requests.txt 保存完整 URL"),
      new Step("9. 產生報告與摘要", "target/gatling/<run>/index.html", "summarize-report.ps1 -> target/summary.txt")
    };

    int cardX = 120;
    int cardW = 760;
    int cardH = 120;
    int startY = 140;
    int gap = 32;

    Font stepTitleFont = new Font("SansSerif", Font.BOLD, 30);
    Font descFont = new Font("SansSerif", Font.PLAIN, 22);

    for (int i = 0; i < steps.length; i++) {
      int y = startY + i * (cardH + gap);

      g.setColor(card);
      g.fill(new RoundRectangle2D.Double(cardX, y, cardW, cardH, 12, 12));
      g.setColor(stroke);
      g.setStroke(new BasicStroke(2f));
      g.draw(new RoundRectangle2D.Double(cardX, y, cardW, cardH, 12, 12));

      g.setColor(text);
      g.setFont(stepTitleFont);
      g.drawString(steps[i].title, cardX + 26, y + 40);

      g.setColor(subtext);
      g.setFont(descFont);
      g.drawString(steps[i].d1, cardX + 26, y + 76);
      g.drawString(steps[i].d2, cardX + 26, y + 104);

      if (i < steps.length - 1) {
        int xMid = cardX + cardW / 2;
        int y1 = y + cardH;
        int y2 = y + cardH + gap;
        g.setColor(stroke);
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(xMid, y1 + 4, xMid, y2 - 8);

        int ax = xMid;
        int ay = y2 - 8;
        int[] xs = {ax, ax - 9, ax + 9};
        int[] ys = {ay, ay - 14, ay - 14};
        g.fillPolygon(xs, ys, 3);
      }
    }

    g.dispose();
    ImageIO.write(image, "png", new File("flow_zh.png"));
    System.out.println("Wrote flow_zh.png");
  }
}
