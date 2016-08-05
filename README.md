<a href='https://travis-ci.org/analyzedgg/backend'><img src='https://travis-ci.org/analyzedgg/backend.svg?branch=master' alt='Build Status'/></a><a href='https://coveralls.io/github/analyzedgg/backend'><img src='https://coveralls.io/repos/github/analyzedgg/backend/badge.svg?branch=test-all-the-things' alt='Coverage Status' /></a>

# League API
This is the backend for the League API project.

## Requirements
1. SBT
2. CouchDB
    * Make sure it runs on localhost:5984
    * Go to [Futon](http://localhost:5984/_utils/) and create the following databases: `matches-db` and `summoner-db`
3. Riot API development key (get yours at: [developers.riotgames.com](https://developer.riotgames.com/))
    * _(Optional)_ Put it as an environment variable named `RIOT_API_KEY`

## Usage
1. Set the `RIOT_API_KEY` variable (if you did not set it as environment variable):
   ``set RIOT_API_KEY=<your-api-key>``
2. Run it with `sbt run`