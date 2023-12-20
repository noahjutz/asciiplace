import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.function.Consumer;

record Pixel(int x, int y, char c) {
}

class App {
	public static final String DB_URL = "jdbc:postgresql://hera.hs-regensburg.de/jun31399?currentSchema=asciiplace";
	public static final String DB_USER = "jun31399";
	public static final String DB_PASS = "jadmsfvhetgthtekjrjeuheasddoucqy";

	public static final int SCREEN_WIDTH = 25;
	public static final int SCREEN_HEIGHT = 15;

	public static Connection connection;

	record A(int x) {
	};

	public static void main(String[] args) {
		init();

		// rendering tests
		// for (int y = 0; y < 100; y++) {
		// for (int x = 0; x < 1; x++) {
		// render(x, y);
		// try {
		// Thread.sleep(50);
		// } catch (InterruptedException e) {
		// }
		// }
		// }

		// fetching tests
		// var pixels = fetch(0, CANVAS_WIDTH, 0, CANVAS_HEIGHT);
		// for (var pixel : pixels) {
		// System.out.println(pixel);
		// }

		// keypress callback tests
		// onKeyPress(c -> System.out.println(c));

		var position = new Object() {
			int x = 0;
			int y = 0;
		};

		onKeyPress(c -> {
			switch (c) {
				case 'w' -> position.y--;
				case 'a' -> position.x--;
				case 's' -> position.y++;
				case 'd' -> position.x++;
			}
		});

		while (true) {
			render(position.x, position.y);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	public static void init() {
		try {
			connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void render(int px, int py) {
		clear();

		final var xMin = px - SCREEN_WIDTH / 2;
		final var xMax = px + SCREEN_WIDTH / 2;
		final var yMin = py - SCREEN_HEIGHT / 2;
		final var yMax = py + SCREEN_HEIGHT / 2;

		for (int y = yMin; y < yMax; y++) {
			for (int x = xMin; x < xMax; x++) {
				char c;
				if (y == py && x == px)
					c = 'X';
				else
					c = '.'; // todo get from db

				System.out.print(c);
			}
			System.out.println();
		}
	}

	public static void clear() {
		System.out.print("\033[H\033[2J");
	}

	public static Pixel[] fetch(int xMin, int xMax, int yMin, int yMax) {
		var pixels = new ArrayList<Record>();

		try (var st = connection.prepareStatement("""
				SELECT DISTINCT ON (x, y) x, y, c
				FROM pixels
				WHERE x >= ?
				AND x < ?
				AND y >= ?
				AND y < ?
				ORDER BY x, y, created_at DESC;
				""")) {
			st.setInt(1, xMin);
			st.setInt(2, xMax);
			st.setInt(3, yMin);
			st.setInt(4, yMax);
			var rs = st.executeQuery();
			while (rs.next()) {
				final var x = rs.getInt(1);
				final var y = rs.getInt(2);
				final var c = rs.getString(3).charAt(0);

				final var pixel = new Pixel(x, y, c);
				pixels.add(pixel);
			}
		} catch (SQLException e) {
			// TODO: handle exception
		}

		return pixels.toArray(new Pixel[pixels.size()]);
	}

	public static void onKeyPress(Consumer<Character> callback) {
		new Thread(() -> {
			while (true) {
				try {
					var c = (char) System.in.read();
					callback.accept(Character.toLowerCase(c));
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}).start();
	}
}
