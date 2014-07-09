require 'bundler/setup'
require 'fileutils'
require 's3_deployer/tasks'
require './s3_deployer_config'

def execute(cmd)
  puts "Running '#{cmd}'"
  system(cmd, out: $stdout, err: :out)
end

def recreate_dir(dir)
  FileUtils.rm_rf(dir)
  FileUtils.mkdir_p(dir)
end

def copy_file_or_directory(source, target)
  puts "Copy #{source} to #{target}"
  FileUtils.cp_r(source, target)
end

def invoke_task(task)
  Rake::Task[task].invoke
end

desc "Compile"
task :compile do
  execute("lein cljsbuild once release")
end

desc "Package"
task :package do
  build_dir = File.expand_path("..", __FILE__)
  dist_dir = "dist"

  recreate_dir(dist_dir)
  %w{
    resources index_prod.html tixi_prod.js tixi_prod.js.map
  }.each do |file|
    copy_file_or_directory(File.join(build_dir, file), dist_dir)
  end
  FileUtils.mv(File.join(dist_dir, "index_prod.html"), File.join(dist_dir, "index.html"))
end

desc "Release"
task :release do
  invoke_task :compile
  invoke_task :package
end

desc "Deploy"
task :deploy do
  invoke_task :release
  invoke_task :"s3_deployer:deploy"
end

