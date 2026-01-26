package com.example.teoat.ui.calendar

import android.graphics.Color
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan

class EventDotDecorator(private val dates: Collection<CalendarDay>) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        // 👇 여기에 Color.RED(빨강)나 Color.BLUE(파랑)가 꼭 있어야 보입니다!
        view.addSpan(DotSpan(5f, Color.RED))
    }
}