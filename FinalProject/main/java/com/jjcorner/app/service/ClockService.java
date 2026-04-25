package com.jjcorner.app.service;

import java.time.Duration;

/**
 * Wraps clock-in and clock-out state and records those events in the activity log.
 */
public final class ClockService {
    private final SessionManager session;
    private final ActivityService activity;

    public ClockService(SessionManager session, ActivityService activity) {
        this.session = session;
        this.activity = activity;
    }

    public void clockIn() {
        session.clockIn();
        activity.record(session.currentUser(), "Clocked in");
    }

    public void clockOut() {
        activity.record(session.currentUser(), "Clocked out after " + elapsedText());
        session.clockOut();
    }

    public boolean isClockedIn() {
        return session.isClockedIn();
    }

    public String elapsedText() {
        Duration d = session.elapsedClockedIn();
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}

