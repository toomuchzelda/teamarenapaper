import me.toomuchzelda.teamarenapaper.utils.ConfigOptional;
import me.toomuchzelda.teamarenapaper.utils.ConfigPath;
import me.toomuchzelda.teamarenapaper.utils.ConfigUtils;
import me.toomuchzelda.teamarenapaper.utils.IntBoundingBox;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigParsingTests {
	private static <T> T parse(String input, Class<T> clazz) {
		Map<?, ?> yaml = new Yaml().load(input);
		return ConfigUtils.parseConfig(yaml, clazz);
	}

	public static class Klass {
		public int i;
		public boolean b;
		public double d;

		public IntBoundingBox ibb;

		public record Nested(List<String> strings, @ConfigPath("__umm-ackshually") String specialAgain) {}
		public Nested nested;
		@ConfigPath("lines")
		public String special;
	}
	@Test
	public void parseClass() {
		String input = """
			i: 69
			b: true
			d: 1.1
			ibb:
			  from: 0,0,0
			  to: 100,100,100
			nested:
			  strings:
			  - a string
			  - another string
			  __umm-ackshually: it's a string
			lines: ok
			""";
		Klass klass = parse(input, Klass.class);
		assertEquals(69, klass.i);
		assertTrue(klass.b);
		assertEquals(1.1, klass.d);
		assertEquals(new IntBoundingBox(0, 0, 0, 100, 100, 100), klass.ibb);
		assertEquals(new Klass.Nested(List.of("a string", "another string"), "it's a string"), klass.nested);
		assertEquals("ok", klass.special);
	}

	@Test
	public void parseOptional() {
		record Optional(@ConfigOptional String s, @ConfigOptional Integer n) {}
		Optional a = parse("s: amogus", Optional.class);
		assertEquals(new Optional("amogus", null), a);
		Optional b = parse("hi: true\nn: 12", Optional.class);
		assertEquals(new Optional(null, 12), b);

		record NotOptional(@ConfigOptional String s, Integer n) {}
		assertThrows(IllegalArgumentException.class, () -> parse("s: 123", NotOptional.class));
	}

	@Test
	public void parseDeeplyNested() {
		record A(int n) {}
		record AContainer(List<A> as) {}
		record AContainerMapper(String condition, Map<String, AContainer> containers) {}
		record Document(List<AContainerMapper> mappers) {}
		String input = """
		mappers:
		- condition: "abc"
		  containers:
		    a:
		      as:
		      - n: 1
		      - n: 2
		      - n: 3
		    b:
		      as:
		      - n: 4
		- condition: "!abc"
		  containers:
		    z:
		      as:
		      - n: -1
		      - n: -2
		      - n: -3
		    y:
		      as:
		      - n: -4
		""";
		Document document = parse(input, Document.class);
		assertEquals(new Document(List.of(
			new AContainerMapper("abc", Map.of(
				"a", new AContainer(List.of(new A(1), new A(2), new A(3))),
				"b", new AContainer(List.of(new A(4)))
			)),
			new AContainerMapper("!abc", Map.of(
				"z", new AContainer(List.of(new A(-1), new A(-2), new A(-3))),
				"y", new AContainer(List.of(new A(-4)))
			))
		)), document);

		assertThrows(IllegalArgumentException.class, () -> parse("""
			mappers:
			- lol
			- 123
			""", Document.class));
		assertThrows(IllegalArgumentException.class, () -> parse("""
			mappers:
			- condition: ok
			  containers:
			  - 123
			- condition: "!ok"
			  containers: 1
			""", Document.class));

	}

	static class FinalKlass {
		public final int a;
		public int b;
		public FinalKlass() {
			a = 0;
		}
	}
	@Test
	public void parseFinal() {
		FinalKlass instance = parse("""
			a: 123
			b: 5""", FinalKlass.class);
		assertEquals(123, instance.a);
		assertEquals(5, instance.b);
	}
}
