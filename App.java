import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
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

	record A(int x) {
	};

	public static void main(String[] args) {
		init();

		var position = new Object() {
			int x = 0;
			int y = 0;
		};

		onKeyPress(c -> {
			switch (c) {
				case 'q' -> System.exit(0);
				case 'w' -> position.y--;
				case 'a' -> position.x--;
				case 's' -> position.y++;
				case 'd' -> position.x++;
			}
		});

		while (true) {
			var bounds = new Bounds(
					position.x - SCREEN_WIDTH / 2,
					position.x + SCREEN_WIDTH / 2,
					position.y - SCREEN_HEIGHT / 2,
					position.y + SCREEN_HEIGHT / 2);

			render(new Position(position.x, position.y), bounds);
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

	public static void render(Position player, Bounds bounds) {
		clear();

		for (int y = bounds.yMin(); y < bounds.yMax(); y++) {
			for (int x = bounds.xMin(); x < bounds.xMax(); x++) {
				char c;
				if (y == player.y() && x == player.x())
					c = 'X';
				else
					c = y % 2 == 0 ? '.' : ','; // todo get from db

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
