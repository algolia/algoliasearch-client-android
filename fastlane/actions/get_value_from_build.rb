require 'fileutils'

module Fastlane
  module Actions
    class GetValueFromBuildAction < Action
      def self.run(params)
        app_project_dir ||= params[:app_project_dir]
        value = ""
        found = false
        Dir.glob("#{app_project_dir}/common.gradle") do |path|
          begin
            File.open(path, 'r') do |file|
              file.each_line do |line|
                unless line.include? "#{params[:key]} " and !found
                  next
                end
                components = line.strip.split(' ')
                value = components.last.tr("\"", "").tr("\'", "")
                break
              end
              file.close
            end
          end
        end
        return value
      end

      #####################################################
      # @!group Documentation
      #####################################################
      def self.available_options
        [
          FastlaneCore::ConfigItem.new(key: :app_project_dir,
                                  env_name: "ANDROID_VERSIONING_APP_PROJECT_DIR",
                               description: "The path to the application source folder in the Android project (default: android/app)",
                                  optional: true,
                                      type: String,
                             default_value: "android/app"),
          FastlaneCore::ConfigItem.new(key: :key,
                               description: "The property key",
                                      type: String)

        ]
      end

      def self.authors
        ["Manabu OHTAKE"]
      end

      def self.is_supported?(platform)
        platform == :android
      end
    end
  end
end
