# Keep sessions in the database

For whoever changes login, deploys or the database. Sessions live in Postgres, the one store larder already runs,
because sessions held in the web app's memory were lost on every deploy.

## Context

In 2023 larder moved from one server to two behind a load balancer, and sessions stayed in each server's memory.
Members were logged out whenever a request reached the other server, and every deploy logged out everyone; the pantry
volunteers stopped deploying during opening hours. Two forces judge the options: a deploy must log no one out, and
the volunteers who run larder keep the number of services they operate down.

## Options

- **Sticky sessions at the load balancer**: no code change; lost because a deploy still logs out everyone on the
  restarted server.
- **Redis as a session store**: fast and common; lost on the second force, a service the volunteers would run only
  for sessions.
- **Postgres**: chosen.

## Decision

Sessions live in a Postgres table every web server reads, because it survives a deploy and adds no service to run.

## Consequences

A deploy logs no one out. Each request reads the session from Postgres, a query the database serves from memory at
larder's size.
