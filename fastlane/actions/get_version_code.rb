require 'fileutils'

module Fastlane
  module Actions
    class GetVersionCodeAction < Action
      def self.run(params)
        GetValueFromBuildAction.run(
          app_project_dir: params[:app_project_dir],
          key: "versionCode"
        )
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
                               default_value: "android/app")
        ]
      end

      def self.description
        "Get the version code of your project"
      end

      def self.details
        [
          "This action will return the current version code set on your project's build.gradle."
        ].join(' ')
      end

      def self.output
        [
          ['VERSION_CODE', 'The version code']
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
