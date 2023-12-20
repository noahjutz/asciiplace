import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

record Position(int x, int y) {
}

record Pixel(int x, int y, char c) {
}

record Bounds(int xMin, int xMax, int yMin, int yMax) {
}

class App {
	public static final String DB_URL = "jdbc:postgresql://hera.hs-regensburg.de/jun31399?currentSchema=asciiplace";
	public static final String DB_USER = "jun31399";
	public static final String DB_PASS = "jadmsfvhetgthtekjrjeuheasddoucqy";

	public static final int SCREEN_WIDTH = 35;
	public static final int SCREEN_HEIGHT = 9;

	public static Connection connection;

	public static void main(String[] args) {
		init();

		var data = new Object() {
			int x = 0;
			int y = 0;
			boolean isInsertMode = false;

			public Position getPosition() {
				return new Position(x, y);
			}
		};

		onKeyPress(c -> {
			if (c == 'i') {
				data.isInsertMode = !data.isInsertMode;
				return;
			}

			if (data.isInsertMode) {
				write(data.getPosition(), c);
				data.isInsertMode = true;
				return;
			}

			switch (c) {
				case 'q' -> System.exit(0);
				case 'w' -> data.y--;
				case 'a' -> data.x--;
				case 's' -> data.y++;
				case 'd' -> data.x++;
			}
		});

		while (true) {
			final var bounds = new Bounds(
					data.x - SCREEN_WIDTH / 2,
					data.x + SCREEN_WIDTH / 2,
					data.y - SCREEN_HEIGHT / 2,
					data.y + SCREEN_HEIGHT / 2);

			final var pixels = fetch(bounds);
			render(data.getPosition(), bounds, pixels);

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

	public static void render(Position player, Bounds bounds, Pixel[] pixels) {
		clear();

		for (int y = bounds.yMin(); y < bounds.yMax(); y++) {
			for (int x = bounds.xMin(); x < bounds.xMax(); x++) {
				char c;
				if (y == player.y() && x == player.x())
					c = 'X';
				else {
					final int x1 = x;
					final int y1 = y;
					c = Arrays.stream(pixels)
							.filter(p -> p.x() == x1 && p.y() == y1)
							.findFirst()
							.map(p -> p.c())
							.orElse(' ');
				}

				System.out.print(c);
			}
			System.out.println();
		}
	}

	public static void clear() {
		System.out.print("\033[H\033[2J");
	}

	public static Pixel[] fetch(Bounds bounds) {
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
			st.setInt(1, bounds.xMin());
			st.setInt(2, bounds.xMax());
			st.setInt(3, bounds.yMin());
			st.setInt(4, bounds.yMax());
			var rs = st.executeQuery();
			while (rs.next()) {
				final var x = rs.getInt(1);
				final var y = rs.getInt(2);
				final var c = rs.getString(3).charAt(0);

				final var pixel = new Pixel(x, y, c);
				pixels.add(pixel);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return pixels.toArray(new Pixel[pixels.size()]);
	}

	public static void write(Position position, char c) {
		// TODO
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
