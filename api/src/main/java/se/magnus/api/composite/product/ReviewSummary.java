package se.magnus.api.composite.product;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReviewSummary {

  private final int reviewId;
  private final String author;
  private final String subject;

  private final String content;

  @JsonCreator
  public ReviewSummary(@JsonProperty("reviewId") int reviewId,
      @JsonProperty("author") String author, @JsonProperty("subject") String subject, @JsonProperty("content") String content) {
    this.reviewId = reviewId;
    this.author = author;
    this.subject = subject;
    this.content = content;
  }

  public ReviewSummary(int reviewId, String author, String subject) {
    this(reviewId, author, subject, null);
  }

  public String getContent() { return content; }
  public int getReviewId() {
    return reviewId;
  }

  public String getAuthor() {
    return author;
  }

  public String getSubject() {
    return subject;
  }
}

