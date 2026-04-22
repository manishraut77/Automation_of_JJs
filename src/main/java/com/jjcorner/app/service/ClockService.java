package com.jjcorner.app.service;

import java.time.Duration;

public final class ClockService {
    private final SessionManager session;

    public ClockService(SessionManager session) {
        this.session = session;
    }

    public void clockIn() {
        session.clockIn();
    }

    public void clockOut() {
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

