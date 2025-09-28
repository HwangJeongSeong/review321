package com.team.cafe.Root;

public interface CafeReviewSummary {
    Long getId();                       // id
    String getCafeName();               // cafeName
    Double getAvgRating();              // avgRating
    Long getReviewsCount();             // reviewsCount
    String getPrimaryImageUrl();        // primaryImageUrl
    String getLatestReviewsAuthor();    // latestReviewsAuthor
    String getLatestReviewsContent();   // latestReviewsContent
}
