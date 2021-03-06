package com.eventor.university.view;

import com.eventor.api.annotations.EventListener;
import com.eventor.university.api.CourseResultsSubmitted;
import com.eventor.university.api.FinalExamSubmitted;

import java.util.HashMap;
import java.util.Map;

@EventListener
public class Grades {
    private Map<CourseStudentKey, Integer> gradesByStudent = new HashMap<CourseStudentKey, Integer>();

    public Integer grade(String courseId, String studentId) {
        CourseStudentKey key = new CourseStudentKey(courseId, studentId);
        return gradesByStudent.containsKey(key) ? gradesByStudent.get(key) : 0;
    }


    @EventListener
    public void on(CourseResultsSubmitted evt) {
        gradesByStudent.put(new CourseStudentKey(evt.courseId, evt.studentId), evt.result);
    }

    @EventListener
    public void on(FinalExamSubmitted evt) {
        gradesByStudent.put(new CourseStudentKey(evt.courseId, evt.studentId), evt.result);
    }

    private static class CourseStudentKey {
        String courseId;
        String studentId;

        private CourseStudentKey(String courseId, String studentId) {
            this.courseId = courseId;
            this.studentId = studentId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CourseStudentKey that = (CourseStudentKey) o;

            if (!courseId.equals(that.courseId)) return false;
            if (!studentId.equals(that.studentId)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = courseId.hashCode();
            result = 31 * result + studentId.hashCode();
            return result;
        }
    }
}
