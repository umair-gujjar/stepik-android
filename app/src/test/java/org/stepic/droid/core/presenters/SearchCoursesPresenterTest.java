package org.stepic.droid.core.presenters;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stepic.droid.analytic.Analytic;
import org.stepic.droid.concurrency.MainHandler;
import org.stepic.droid.core.presenters.contracts.CoursesView;
import org.stepic.droid.model.Course;
import org.stepic.droid.model.Meta;
import org.stepic.droid.model.SearchResult;
import org.stepic.droid.test_utils.ConcurrencyUtilForTest;
import org.stepic.droid.test_utils.ResponseGeneratorKt;
import org.stepic.droid.test_utils.generators.FakeCourseGenerator;
import org.stepic.droid.test_utils.generators.FakeMetaGenerator;
import org.stepic.droid.test_utils.generators.FakeSearchResultGenerator;
import org.stepic.droid.util.resolvers.SearchResolver;
import org.stepic.droid.util.resolvers.SearchResolverImpl;
import org.stepic.droid.web.Api;
import org.stepic.droid.web.CoursesStepicResponse;
import org.stepic.droid.web.SearchResultResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchCoursesPresenterTest {

    private SearchCoursesPresenter searchCoursesPresenter;


    @Mock
    Api api;

    @Mock
    ThreadPoolExecutor threadPoolExecutor;

    @Mock
    MainHandler mainHandler;

    SearchResolver searchResolver;

    @Mock
    Analytic analytic;

    @Mock
    CoursesView coursesView;


    @Before
    public void beforeEachTest() {
        MockitoAnnotations.initMocks(this);

        ConcurrencyUtilForTest.transformToBlockingMock(threadPoolExecutor);
        ConcurrencyUtilForTest.transformToBlockingMock(mainHandler);

        searchResolver = spy(new SearchResolverImpl());

        searchCoursesPresenter = new SearchCoursesPresenter(api, threadPoolExecutor, mainHandler, searchResolver, analytic);
    }

    @Test
    public void downloadData_oneCourse_success() throws IOException {

        searchCoursesPresenter.attachView(coursesView);
        String searchQuery = "One course in answer pls";
        Meta onePageMeta = FakeMetaGenerator.INSTANCE.generateFakeMeta();


        //mock calling api for getting search results
        SearchResultResponse responseMock = mock(SearchResultResponse.class);
        List<SearchResult> searchResults = new ArrayList<>();
        int expectedCourseId = 67;
        SearchResult expectedSingleSearchResult = FakeSearchResultGenerator.INSTANCE.generateFakeSearchResult(expectedCourseId);
        searchResults.add(expectedSingleSearchResult);
        when(responseMock.getSearchResultList()).thenReturn(searchResults);
        when(responseMock.getMeta()).thenReturn(onePageMeta);
        ResponseGeneratorKt.useMockInsteadCall(when(api.getSearchResultsCourses(1, searchQuery)), responseMock);


        //mock calling api for getting course
        long[] courseIds = new long[1];
        courseIds[0] = expectedSingleSearchResult.getCourse();
        CoursesStepicResponse coursesStepicResponse = mock(CoursesStepicResponse.class);
        when(coursesStepicResponse.getMeta()).thenReturn(onePageMeta);
        List<Course> expectedCourses = new ArrayList<>();
        Course expectedCourse = FakeCourseGenerator.INSTANCE.generateFakeCourse(expectedCourseId);
        expectedCourses.add(expectedCourse);
        when(coursesStepicResponse.getCourses()).thenReturn(expectedCourses);
        ResponseGeneratorKt.useMockInsteadCall(when(api.getCourses(1, courseIds)), coursesStepicResponse);


        //call method of tested object
        searchCoursesPresenter.downloadData(searchQuery);

        //verify calling of dependencies
        verify(api).getSearchResultsCourses(1, searchQuery);
        verify(api).getCourses(1, courseIds);

        verify(threadPoolExecutor).execute(any(Runnable.class));
        verify(searchResolver).getCourseIdsFromSearchResults(any(List.class));

        //verify calls of view methods
        InOrder inOrder = inOrder(coursesView);
        inOrder.verify(coursesView).showLoading();
        inOrder.verify(coursesView).showCourses(expectedCourses);

        //verify never called view's methods
        verify(coursesView, never()).showConnectionProblem();
        verify(coursesView, never()).showEmptyCourses();
    }
}