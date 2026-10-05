package org.chiterok.grandDuels.arena;

public enum ArenaState {
    /** Free to be used by a new duel. */
    WAITING,
    /** Reserved: players are frozen during the countdown. */
    STARTING,
    /** The fight is running. */
    IN_GAME,
    /** The fight is over: celebration, player restoration and block rollback. */
    RESETTING
}
