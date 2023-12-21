import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

class Env {
	public static final String DB_URL = "jdbc:postgresql://hera.hs-regensburg.de/jun31399?currentSchema=asciiplace";
	public static final String DB_USER = "jun31399";
	public static final String DB_PASS = "jadmsfvhetgthtekjrjeuheasddoucqy";

	public static final int SCREEN_WIDTH = 35;
	public static final int SCREEN_HEIGHT = 9;
}

record Position(int x, int y) {
}

record Pixel(int x, int y, char c) {
}

record Bounds(int xMin, int xMax, int yMin, int yMax) {
}

class App {
	static Connection connection;

	public static void main(String[] args) throws SQLException {
		connection = DriverManager.getConnection(Env.DB_URL, Env.DB_USER, Env.DB_PASS);

		var data = new Object() {
			int x = 0;
			int y = 0;
			boolean isInsertMode = false;

			public Position getPosition() {
				return new Position(x, y);
			}
		};

		onKeyPress(c -> {
			if (data.isInsertMode && (!Character.isWhitespace(c) || c == ' ')) {
				try {
					write(data.getPosition(), c);
				} catch (SQLException e) {
					e.printStackTrace();
					System.exit(1);
				}
				data.isInsertMode = false;
				return;
			}

			switch (Character.toLowerCase(c)) {
				case 'q' -> System.exit(0);
				case 'w' -> data.y--;
				case 'a' -> data.x--;
				case 's' -> data.y++;
				case 'd' -> data.x++;
				case 'i' -> data.isInsertMode = true;
			}
		});

		while (true) {
			final var bounds = new Bounds(
					data.x - Env.SCREEN_WIDTH / 2,
					data.x + Env.SCREEN_WIDTH / 2,
					data.y - Env.SCREEN_HEIGHT / 2,
					data.y + Env.SCREEN_HEIGHT / 2);

			final var pixels = fetch(bounds);

			if (data.isInsertMode) {
				clear();
				System.out.println("Enter a character and press enter.");
			} else {
				render(data.getPosition(), bounds, pixels);
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	static void render(Position player, Bounds bounds, Pixel[] pixels) {
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

	static void clear() {
		System.out.print("\033[H\033[2J");
	}

	static Pixel[] fetch(Bounds bounds) throws SQLException {
		var pixels = new ArrayList<Record>();

		var st = connection.prepareStatement("""
				SELECT DISTINCT ON (x, y) x, y, c
				FROM pixels
				WHERE x >= ? AND x < ?
				AND y >= ? AND y < ?
				ORDER BY x, y, created_at DESC;
				""");

		st.setInt(1, bounds.xMin());
		st.setInt(2, bounds.xMax());
		st.setInt(3, bounds.yMin());
		st.setInt(4, bounds.yMax());

		var rs = st.executeQuery();
		while (rs.next())
			pixels.add(new Pixel(rs.getInt(1), rs.getInt(2), rs.getString(3).charAt(0)));

		st.close();
		rs.close();

		return pixels.toArray(new Pixel[pixels.size()]);
	}

	static void write(Position position, char c) throws SQLException {
		var st = connection.prepareStatement("INSERT INTO pixels (x, y, c) VALUES (?, ?, ?)");
		System.out.println(c);
		st.setInt(1, position.x());
		st.setInt(2, position.y());
		st.setObject(3, c);
		st.execute();

		st.close();
	}

	static void onKeyPress(Consumer<Character> callback) {
		new Thread(() -> {
			while (true) {
				try {
					var c = (char) System.in.read();
					callback.accept(c);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}).start();
	}
}
