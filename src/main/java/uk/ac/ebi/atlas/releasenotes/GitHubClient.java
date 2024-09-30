package uk.ac.ebi.atlas.releasenotes;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import uk.ac.ebi.atlas.releasenotes.records.Commit;
import uk.ac.ebi.atlas.releasenotes.records.GitHubProject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public interface GitHubClient {
    int DEFAULT_PAGE_SIZE = 30;

    @RequestLine("GET /repos/{owner}/{repo}")
    @Headers({
            "Accept: application/vnd.github+json",
            "X-GitHub-Api-Version: 2022-11-28",
    })
    GitHubProject getProject(@Param("owner") String owner, @Param("repo") String repo);

    @RequestLine("GET /repos/{owner}/{repo}/commits?sha={sha}&page={page}")
    @Headers({
            "Accept: application/vnd.github+json",
            "X-GitHub-Api-Version: 2022-11-28",
    })
    List<Commit> getCommitsPage(@Param("owner") String owner, @Param("repo") String repo, @Param("sha") String sha, @Param("page") int page);

    default List<Commit> getCommits(String owner, String repo, String sha) {
        return fetchAllPages(DEFAULT_PAGE_SIZE, page -> getCommitsPage(owner, repo, sha, page));
    }

    default <T> List<T> fetchAllPages(int pageSize, IntFunction<List<T>> pageFunction) {
        List<T> allResults = new ArrayList<>();
        List<T> curPageData = null;
        for (int curPageNum = 1; curPageData == null || curPageData.size() == pageSize; curPageNum++) {
            curPageData = pageFunction.apply(curPageNum);
            allResults.addAll(curPageData);
        }

        return allResults;
    }
}
