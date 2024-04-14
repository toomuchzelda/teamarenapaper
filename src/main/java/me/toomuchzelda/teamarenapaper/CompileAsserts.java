package me.toomuchzelda.teamarenapaper;

/**
 * This class contains a boolean field that's checked before doing any debugging assertions.
 * The reason for doing this instead of just asserting regularly is if the boolean is set
 * to true the compiler will optimize out any code that depended on it, making the program
 * slightly faster.
 */
public class CompileAsserts
{
	public static final boolean OMIT = false;
}
