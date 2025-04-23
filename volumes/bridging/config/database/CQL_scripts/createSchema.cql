------------------------------------------------------
----------------  KEYSPACE CREATION ------------------
------------------------------------------------------

-- We use SimpleStrategy because in NOT-PRO environment we don't have several data-centers.
CREATE KEYSPACE IF NOT EXISTS dmtool_dev 
WITH REPLICATION = { 'class' : 'SimpleStrategy',  'replication_factor': 3};

-- NOTE: SimpleStrategy is a replication factor for the entire cluster, independently of the topology. It must be used just as pedagogical aim.

------------------------------------------------------
-----------------  TABLES CREATION -------------------
------------------------------------------------------
USE dmtool_dev;


CREATE TABLE networkids (
                typeOfIdentifier text,
                identifier text,
                msisdn text,
				hashMsisdn  text,   -- Only for  type=MSISDN (SHA256)
                status text,        -- LEGACY | DBSS //Only for  type=MSISDN
                locationInfo blob,
                PRIMARY KEY ((typeOfIdentifier, identifier))
); 	-- for  query number 1 and 2 
	-- If type=’MSISDN’ then number=msisdn

CREATE INDEX status_idx ON dmtool_dev.networkids(status);

CREATE TABLE  mapping (
                source text,                        -- Legacy Operation
                resource text,                   	-- Legacy Operation message under Legacy
                data blob,
                PRIMARY KEY ((source, resource))
); -- for  query number 3


CREATE TABLE brAuthentication (
                user text,
                pass text,
                PRIMARY KEY ((user, pass))
);

