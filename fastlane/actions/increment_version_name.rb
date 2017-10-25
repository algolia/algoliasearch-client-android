require 'tempfile'
require 'fileutils'

module Fastlane
  module Actions
    module SharedValues
      VERSION_NAME = :VERSION_NAME
    end
    class IncrementVersionNameAction < Action
      def self.run(params)
        if params[:version_name].nil? or params[:version_name].empty?
          current_version = GetVersionNameAction.run(params)
          UI.user_error!("Your current version (#{current_version}) does not respect the format A.B.C") unless current_version =~ /\d+.\d+.\d+/
          version = current_version.split(".").map(&:to_i)
          case params[:bump_type]
          when "patch"
            version[2] = version[2] + 1
            new_version = version.join(".")
          when "minor"
            version[1] = version[1] + 1
            version[2] = version[2] = 0
            new_version = version.join(".")
          when "major"
            version[0] = version[0] + 1
            version[1] = 0
            version[2] = 0
            new_version = version.join(".")
          end
        else
          new_version = params[:version_name]
        end
        puts "new version " + new_version + " old version " + current_version
        SetValueInBuildAction.run(
          app_project_dir: params[:app_project_dir],
          key: "versionName",
          value: new_version
        )
        Actions.lane_context[SharedValues::VERSION_NAME] = new_version
        new_version
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
          FastlaneCore::ConfigItem.new(key: :bump_type,
                                  env_name: "ANDROID_VERSIONING_BUMP_TYPE",
                               description: "Change to a specific type (optional)",
                                  optional: true,
                                      type: String,
                             default_value: "patch",
                              verify_block: proc do |value|
                                              UI.user_error!("Available values are 'patch', 'minor' and 'major'") unless ['patch', 'minor', 'major'].include? value
                                            end),
          FastlaneCore::ConfigItem.new(key: :version_name,
                                  env_name: "ANDROID_VERSIONING_VERSION_NAME",
                               description: "Change to a specific version (optional)",
                                  optional: true,
                                      type: String)
        ]
      end

      def self.description
        "Increment the version name of your project"
      end

      def self.details
        [
          "This action will increment the version name directly in build.gradle."
        ].join("\n")
      end

      def self.output
        [
          ['VERSION_NAME', 'The new version name']
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
