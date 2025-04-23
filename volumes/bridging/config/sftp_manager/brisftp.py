import yaml
import pysftp
import shutil
import sys
import logging
import os, fnmatch

logger = logging.getLogger("brisftp")
transfer_total = 0
transfer_success = 0
transfer_failed = 0

def init_logger(level):
	logger.setLevel(logging.DEBUG)
	fh = logging.FileHandler('brisftp.log')
	formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
	# create file handler
	fh.setFormatter(formatter)
	fh.setLevel(logging.DEBUG)
	# create console handler with a higher log level
	ch = logging.StreamHandler()
	ch.setLevel(logging.INFO)
	logger.addHandler(fh)
	logger.addHandler(ch)
	logger.debug("Logger initialized.")

def load_config(config_file):
	with open(config_file, "r") as file_descriptor:
		data = yaml.load(file_descriptor)
	return data

def get_source_info(metadata):
	sources = metadata.get("source")
	source_resources = metadata.get("source").get("resources")
	return source_resources

def printit(obj):
	if isinstance(obj, list):
		for i in obj:
			printit(i)
	elif isinstance(obj, dict):
		for k, v in obj.items():
			logger.info(" %s = %s " % (k, v))
	else:
		logger.info(obj)

def connect(access_data):
	hostname = str(access_data.get("hostname"))
	if hostname == "localhost":
		return
	user = access_data.get("user")
	pswd = access_data.get("password")
	server = pysftp.Connection(host=hostname, username=user, password=pswd)
	return server

def transfer(source, target, action):
	return_val = False
	if action is None:
		action = "copy"
	if action != "copy" and action != "move":
		logger.error("ERROR: Could not recognize action %r" % action)

	hostname = target.get("hostname")
	target_path = target.get("path")
	if( hostname == "localhost" ):
		logger.debug("Locally copy source [%r] to target [%r] ..." % (source, target))
		try:
			if action == "copy":
				shutil.copy2(source, target_path)
			else:
				shutil.move(source, target_path)
			return_val = True
		except IOError as ioe:
			logger.error("ERROR: %s" % str(ioe))
	else:
		user = target.get("user")
		pswd = target.get("password")
		logger.debug("SFTP From [%r] To [%r@%r:%r] ..." % (source, user, hostname, target_path))
		try:
			# disable host key checking
			cnopts = pysftp.CnOpts()
			cnopts.hostkeys = None
			with pysftp.Connection(host=hostname, username=user, password=pswd, cnopts=cnopts) as sftp:
				try:
					with sftp.cd(target_path):
						sftp.put(source)
						return_val = True
				except IOError:
					logger.error("ERROR: Remote path %r doesn't exist." % target_path)
				except OSError:
					logger.error("ERROR: Local path %r doesn't exist." % source)
		except pysftp.AuthenticationException:
			logger.error("ERROR: Authentication failed for %s@%s"% (user, hostname))
		except pysftp.SSHException as sshe:
			logger.error("ERROR: Unable to connect to %r. Reason : %s" % (hostname, str(sshe)))
	return return_val

def execute(config_data):
	sources = config_data.get("source")
	for source_key, source_data in sources.items():
		logger.info("Prcessing source :: %r ..." % source_key)
		source_resoures = source_data.get("resources") # get_source_info(source_data)
		targets = config_data.get("targets")

		for resource in source_resoures:
			if isinstance(resource, dict):
				for resource_key, resournce_data in resource.items():
					logger.info("Processing resource :: %s -> %s ..." %(source_key, resource_key))
					action = resournce_data.get("action")
					src_dir = resournce_data.get("path")
					target_ids = resournce_data.get("target")
					files = resournce_data.get("files")
					if not files:
						logger.error("ERROR : Please specify files for  %r -> %r"%(source_key, resource_key))
						return
					for file_name in files:
						logger.info("Processing file :: %s -> %s -> %s ..." %(source_key, resource_key, file_name))
						# Transfer each file to the targets associated with target_ids
						# Each file name may contain wildcards which may result to multiple files
						# Get list of files each file name points to (it may be one or more than one)
						files_to_transfer = get_file_list(src_dir, file_name)
						logger.debug("Files to transfer {}".format(files_to_transfer))
						for file in files_to_transfer:
							for target_id in target_ids:
								target = targets.get(target_id)
								if target is None:
									logger.error("ERROR: Target '%r' not found." % target_id)
									return
								source = src_dir
								if file:
									source = source + os.sep + file
								global transfer_total
								transfer_total += 1
								transferSeccess = transfer(source, target, action)
								if transferSeccess:
									global transfer_success
									transfer_success += 1
									logger.info("Successfully transferred resource %r (%r) from %r to %r ..." % (resource_key, file, source_key, target_id))
								else:
									global transfer_failed
									transfer_failed += 1
									logger.info("Faield to transfer resource %r from %r to %r ..." % (resource_key, source_key, target_id))
			else:
				logger.info("Please specify 'resources' to be copied/moved.")

def get_file_list(src_dir, file_name):
	files = []
	for file in os.listdir(src_dir):
		if fnmatch.fnmatch(file, file_name):
			files.append(file)
	return files

if __name__ == "__main__":
	init_logger(logging.INFO)
	logger.info("----------------- Start -----------------")
	if ( len(sys.argv) < 2 ):
		logger.info("Usage python sftp <yaml_configuration_file_path>")
		sys.exit(1)
	else:
		config_filepath = sys.argv[1]

	if( config_filepath is None or str(config_filepath).endswith("yaml") is False):
		logger.error("ERROR: Configuration file is not specified.")
		sys.exit(1)

	logger.info("Loading configuration data from file %r ..." % config_filepath)
	#config_filepath = "sftp-config.yaml"


	try:
		config_data = load_config(config_filepath)
		logger.info("starting execution ...")
		execute(config_data)
	except IOError as ioe:
		#logger.error("ERROR: Unable to locate the YAML configuration file.")
		logger.error("IOError: {}".format(str(ioe)))
	except yaml.parser.ParserError:
		logger.error("ERROR: Unable to parse YAML configuration data.")

	#logger.info("Total Resources to transfer [%d], Success [%d] Failure [%d]" % (transfer_total, transfer_success, transfer_failed))
	logger.info("--------------------------------------------------------------------------")
	logger.info("Successfully transferred %d out of %d resources." % (transfer_success, transfer_total))
	if transfer_failed > 0:
		logger.warning("Failed to transfer %d resource(s). Please see log file for error details."%transfer_failed);
	logger.info("--------------------------------------------------------------------------")

	logger.info("----------------- End -----------------")

