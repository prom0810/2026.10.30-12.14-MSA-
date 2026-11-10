package se.magnus.api.composite.product;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RecommendationSummary {

  private final int recommendationId;
  private final String author;
  private final int rate;

  private final String content;

  @JsonCreator
  public RecommendationSummary(@JsonProperty("recommendationId") int recommendationId,
      @JsonProperty("author") String author, @JsonProperty("rate") int rate, @JsonProperty("content") String content) {
    this.recommendationId = recommendationId;
    this.author = author;
    this.rate = rate;
    this.content = content;
  }

  public RecommendationSummary(int recommendationId, String author, int rate) {
    this(recommendationId, author, rate, null);
  }

  public String getContent() { return content; }
  public int getRecommendationId() {
    return recommendationId;
  }

  public String getAuthor() {
    return author;
  }

  public int getRate() {
    return rate;
  }
}

