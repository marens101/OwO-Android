package me.bramhaag.owoandroid.util

interface Consumer<in T> {

    /**
     * Performs this operation on the given argument.

     * @param t the input argument
     */
    fun accept(t: T)
}
