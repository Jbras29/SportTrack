package com.jocf.sporttrack.service;

/**
 * Levée lorsqu'un utilisateur tente une opération sur un commentaire / réaction dont il n'est pas l'auteur.
 */
public class NonAutoriseException extends RuntimeException {

    public NonAutoriseException(String message) {
        super(message);
    }
}
