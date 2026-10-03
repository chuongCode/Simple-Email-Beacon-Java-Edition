<p align="center">
  <img src="Project Elements/Simple_Email_Beacon.png"/>
</p>

<hr>
<br/>

`SEB` is a proof of concept on a method of measuring email activity. Previously used to track analytics of U of R's Computer Science Undergraduate Council mailing list.

This repo is a Java/Spring port and consolidation of the original [Simple Email Beacon](https://github.com/chuongCode/Simple-Email-Beacon).

## Stack

- Java 21
- Spring Boot 4.1
- Spring Web MVC and `JdbcClient`
- SQLite with Flyway migrations
- React 18 / Create React App (carried over from the frontend repository)

`JdbcClient` is used instead of JPA so the Spring implementation could keep the original SQLite table and column names. This made an existing `database.db` straightforward to carry forward.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the project layout, APIs, database migration process, and runtime configuration.

## Run the complete application

Install a Java 21 JDK, then run:

```bash
./mvnw clean package
java -jar target/email-beacon-0.1.0-SNAPSHOT.jar
```

Open <http://localhost:8080>. The Maven build downloads its own Maven, Node, and npm distributions; only Java is required on the host.

The dashboard loads saved beacons from SQLite, refreshes their status every 15 seconds, and lets you rename or delete them. Use **Copy URL** for the raw tracking URL, **Copy HTML** for a ready-to-paste image tag, **Test hit** to verify tracking without changing the estimated-open count, and **Visits** to inspect recorded loads.

### How tracking accuracy works

Every image request is retained as a raw load, then labeled as likely human, mail proxy, automated, test, or unknown. Requests from the same hashed IP-address/user-agent combination inside the deduplication window are also marked as duplicates. The dashboard uses those signals to show:

- **Total loads:** every request received by the pixel endpoint.
- **Estimated unique opens:** distinct visitors after excluding known automated and test loads.
- **Likely human:** loads whose user agent looks like a normal browser or mail client.
- **Mail proxy:** loads identified as an email provider's image proxy.
- **Automated:** recognizable bots, scanners, previews, and command-line clients.
- **Duplicate:** a repeated visitor fingerprint during the configured time window.
- **Test:** loads created with the dashboard's test action.

These are transparent heuristics, not proof that a particular person read an email. Mail providers can prefetch or proxy images, privacy features can hide the recipient's real address and device, and some clients block remote images entirely. The app therefore preserves raw counts and labels uncertain traffic instead of presenting every pixel request as a confirmed human open.

For backend-only development, skip the React build:

```bash
./mvnw -Dskip.frontend=true spring-boot:run
```

For React live reload, run the backend command above and then:

```bash
cd frontend
npm ci
npm start
```

The development server proxies API calls to Spring on port 8080.

## Verification

```bash
./mvnw test -Dskip.frontend=true
```

The integration suite covers generation, unread status, tracking-pixel delivery, visit persistence, load classification, duplicate detection, test loads, list/create JSON flows, and the default refusal to trust spoofable forwarded-IP headers.
