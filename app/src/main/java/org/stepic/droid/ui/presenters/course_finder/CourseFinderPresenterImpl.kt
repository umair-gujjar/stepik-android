package org.stepic.droid.ui.presenters.course_finder

import org.stepic.droid.base.MainApplication
import org.stepic.droid.concurrency.IMainHandler
import org.stepic.droid.events.courses.CourseCantLoadEvent
import org.stepic.droid.events.courses.CourseFoundEvent
import org.stepic.droid.events.courses.CourseUnavailableForUserEvent
import org.stepic.droid.store.operations.DatabaseFacade
import org.stepic.droid.ui.abstraction.LoadCourseView
import org.stepic.droid.ui.presenters.PresenterImpl
import org.stepic.droid.web.CoursesStepicResponse
import org.stepic.droid.web.IApi
import retrofit.Callback
import retrofit.Response
import retrofit.Retrofit
import java.util.concurrent.ThreadPoolExecutor
import javax.inject.Inject

open class CourseFinderPresenterImpl
@Inject
constructor() : PresenterImpl<LoadCourseView>(), CourseFinderPresenter {
    @Inject
    lateinit var mThreadPoolExecutor: ThreadPoolExecutor

    @Inject
    lateinit var mDatabaseFacade: DatabaseFacade

    @Inject
    lateinit var api: IApi

    @Inject
    lateinit var mainHandler: IMainHandler

    init {
        MainApplication.component().inject(this)
    }

    override fun findCourseById(courseId: Long) {
        mThreadPoolExecutor.execute {
            var course = mDatabaseFacade.getCourseById(courseId, DatabaseFacade.Table.featured)
            if (course == null) {
                course = mDatabaseFacade.getCourseById(courseId, DatabaseFacade.Table.enrolled)
            }

            val finalCourse = course
            if (finalCourse != null) {
                mainHandler.post {
                    view?.onCourseFound(CourseFoundEvent(finalCourse))
                }
            } else {
                api.getCourse(courseId).enqueue(object : Callback<CoursesStepicResponse> {
                    override fun onResponse(response: Response<CoursesStepicResponse>, retrofit: Retrofit) {
                        if (response.isSuccess && !response.body().courses.isEmpty()) {
                            view?.onCourseFound(CourseFoundEvent(response.body().courses[0]))
                        } else {
                            view?.onCourseUnavailable(CourseUnavailableForUserEvent(courseId))

                        }
                    }

                    override fun onFailure(t: Throwable) {
                        view!!.onInternetFailWhenCourseIsTriedToLoad(CourseCantLoadEvent())
                    }
                })
            }
        }
    }
}
